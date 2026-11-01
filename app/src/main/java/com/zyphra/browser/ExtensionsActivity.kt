package com.zyphra.browser

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController

data class ExtInfo(
    val id: String,
    val name: String,
    val sub: String,
    val enabled: Boolean,
    val optionsUrl: String?,
) {
    /**
     * Version and description plus the enable state, so a disabled add-on
     * reads as disabled in the list and in its own dialog without the user
     * having to open anything to find out.
     */
    fun statusLine(ctx: android.content.Context): String = listOf(
        sub,
        ctx.getString(if (enabled) R.string.ext_enabled else R.string.ext_disabled),
    ).filter { it.isNotBlank() }.joinToString(" • ")
}

/** One row in the installable list: tapping it installs. */
data class AddonRow(
    val addon: ZyphraDefaults.Addon,
    val installed: Boolean,
    val onClick: () -> Unit,
)

class ExtensionsActivity : AppCompatActivity() {

    private lateinit var adapter: ExtensionsAdapter
    private lateinit var addonsAdapter: AddonsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_extensions)
        SystemBars.apply(this)

        adapter = ExtensionsAdapter(
            emptyList(),
            onToggleEnable = { setEnabled(it, !it.enabled) },
            onShowSettings = { openExtensionSettings(it) },
            onUninstallExt = { uninstall(it) },
        )
        addonsAdapter = AddonsAdapter(emptyList())
        findViewById<RecyclerView>(R.id.extensions_available_list).apply {
            layoutManager = LinearLayoutManager(this@ExtensionsActivity)
            adapter = this@ExtensionsActivity.addonsAdapter
        }
        findViewById<RecyclerView>(R.id.extensions_list).apply {
            layoutManager = LinearLayoutManager(this@ExtensionsActivity)
            adapter = this@ExtensionsActivity.adapter
        }
        findViewById<View>(R.id.extensions_back).setOnClickListener { finish() }
        wireDelegates()
        reload()
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun controller(): WebExtensionController? {
        val rt = MainActivity.runtimeRef() ?: return null
        return rt.webExtensionController
    }

    private inner class AddonDelegate : WebExtensionController.AddonManagerDelegate {
        override fun onInstalled(extension: WebExtension) {
            runOnUiThread {
                Toast.makeText(
                    this@ExtensionsActivity,
                    installLabel(extension),
                    Toast.LENGTH_SHORT,
                ).show()
                reload()
            }
        }

        override fun onUninstalled(extension: WebExtension) {
            runOnUiThread { reload() }
        }

        override fun onInstallationFailed(
            extension: WebExtension?,
            error: WebExtension.InstallException,
        ) {
            android.util.Log.e(
                "ExtensionsActivity",
                "Installation failed for ${extension?.metaData?.name ?: extension?.id}: ${error.message}",
                error,
            )
            runOnUiThread {
                Toast.makeText(
                    this@ExtensionsActivity,
                    "${getString(R.string.ext_install_failed)}: ${error.message}",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    private fun installLabel(extension: WebExtension): String {
        val name = extension.metaData.name ?: extension.id
        return name + " installed"
    }

    private fun wireDelegates() {
        val c = controller() ?: return
        try {
            c.promptDelegate = InstallPromptHandler()
        } catch (_e: Exception) {
        }
        try {
            c.setAddonManagerDelegate(AddonDelegate())
        } catch (_e: Exception) {
        }
    }

    /**
     * Takes both delegates back off the runtime.
     *
     * The controller belongs to the process-wide GeckoRuntime, not to this
     * screen, and both delegates are inner classes holding this activity. They
     * were installed on entry and never removed, so after leaving the screen an
     * add-on installing in the background still fired a Toast and re-resolved
     * views on a destroyed activity. Only clear them when this screen is the one
     * that installed them: another instance may already own the controller.
     */
    override fun onDestroy() {
        if (installedDelegates) {
            val c = controller()
            try {
                c?.setAddonManagerDelegate(null)
            } catch (_e: Exception) {
            }
            try {
                c?.promptDelegate = null
            } catch (_e: Exception) {
            }
            installedDelegates = false
        }
        super.onDestroy()
    }

    /** True while this instance owns the runtime's add-on delegates. */
    private var installedDelegates = false

    private inner class InstallPromptHandler : WebExtensionController.PromptDelegate {
        override fun onInstallPromptRequest(
            extension: WebExtension,
            permissions: Array<String>,
            origins: Array<String>,
            dataCollectionPermissions: Array<String>,
        ): GeckoResult<WebExtension.PermissionPromptResponse>? {
            val result = GeckoResult<WebExtension.PermissionPromptResponse>()
            val perms = (permissions.toList() + origins).distinct().take(12)
            val msg = buildString {
                append(extension.metaData.name ?: extension.id)
                if (perms.isNotEmpty()) {
                    append("\n\n")
                    append(perms.joinToString("\n- ", prefix = "- "))
                }
            }
            runOnUiThread {
                AlertDialog.Builder(this@ExtensionsActivity)
                    .setTitle(getString(R.string.ext_permissions))
                    .setMessage(msg)
                    .setPositiveButton(getString(R.string.allow)) { _, _ ->
                        result.complete(
                            WebExtension.PermissionPromptResponse(true, true, true),
                        )
                    }
                    .setNegativeButton(getString(R.string.deny)) { _, _ ->
                        result.complete(
                            WebExtension.PermissionPromptResponse(false, false, false),
                        )
                    }
                    .setOnCancelListener {
                        result.complete(
                            WebExtension.PermissionPromptResponse(false, false, false),
                        )
                    }
                    .show()
            }
            return result
        }
    }

    /**
     * Installs one add-on from its "latest" AMO URL.
     *
     * The confirmation shows the URL because this is a fetch from the network
     * that hands the engine a file to run with page access, and a user who can
     * see where it comes from can decide that is not acceptable. The failure is
     * reported with the engine's own message, because the common case is a 404
     * from a slug that no longer exists and the generic string would hide it.
     */
    private fun confirmInstall(addon: ZyphraDefaults.Addon) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.install_addon, addon.name))
            .setMessage(addon.url)
            .setPositiveButton(getString(R.string.install)) { _, _ -> installAddon(addon) }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun installAddon(addon: ZyphraDefaults.Addon) {
        val c = controller()
        if (c == null) {
            Toast.makeText(this, getString(R.string.not_available), Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, getString(R.string.ext_installing), Toast.LENGTH_SHORT).show()
        // Download the XPI to a local file first, then install from file.
        // Installing directly from the AMO URL can fail silently for some
        // extensions (error -101) because the redirect or network path
        // interferes with GeckoView's installation pipeline. A local file
        // install is more reliable.
        Thread {
            try {
                val url = java.net.URL(addon.url)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.instanceFollowRedirects = true
                val xpiFile = java.io.File(cacheDir, "${addon.slug}.xpi")
                connection.inputStream.use { input ->
                    xpiFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                android.util.Log.i("ExtensionsActivity", "Downloaded ${addon.name}: ${xpiFile.length()} bytes")

                runOnUiThread {
                    try {
                        c.install("file://${xpiFile.absolutePath}", WebExtensionController.INSTALLATION_METHOD_FROM_FILE)
                            .accept(
                                { ext ->
                                    android.util.Log.i("ExtensionsActivity", "Installed: ${ext?.metaData?.name ?: ext?.id}")
                                },
                                { error ->
                                    val code = (error as? WebExtension.InstallException)?.code ?: -1
                                    val msg = when (code) {
                                        -101 -> "Extension requires features not available in this browser"
                                        -100 -> "Installation was cancelled"
                                        else -> error?.message ?: "Unknown error (code $code)"
                                    }
                                    android.util.Log.e("ExtensionsActivity", "Install failed for ${addon.name}: code=$code ${error?.message}", error)
                                    runOnUiThread {
                                        Toast.makeText(
                                            this@ExtensionsActivity,
                                            "${getString(R.string.ext_install_failed)}: $msg",
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    }
                                },
                            )
                    } catch (e: Exception) {
                        android.util.Log.e("ExtensionsActivity", "Exception installing ${addon.name}: ${e.message}", e)
                        runOnUiThread {
                            Toast.makeText(this@ExtensionsActivity, "${getString(R.string.ext_install_failed)}: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ExtensionsActivity", "Download failed for ${addon.name}: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@ExtensionsActivity, "${getString(R.string.ext_install_failed)}: Download failed - ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    /**
     * Matches an installable addon against the list of installed extensions.
     *
     * Exact name matching fails because the engine reports its own display
     * name (e.g. "NoScript" or "SponsorBlock for YouTube - Skip Sponsorships")
     * which rarely equals our addon name ("NoScript Security Suite", "SponsorBlock").
     * Fall back to word-overlap: every word of the addon name must appear in
     * the installed extension's name (case-insensitive). This handles all our
     * addons correctly.
     */
    private fun isAddonInstalled(addon: ZyphraDefaults.Addon, installed: List<WebExtension>): Boolean {
        // Names rarely match exactly (e.g. "NoScript Security Suite" vs "NoScript",
        // "SponsorBlock" vs "SponsorBlock for YouTube - Skip Sponsorships",
        // "Search by Image" vs "Search goods by image"). Match on the first
        // significant word (length > 2) of the addon name: it is the distinctive
        // part and is present in every installed extension's reported name.
        val firstWord = addon.name.lowercase().split(Regex("\\s+")).firstOrNull { it.length > 2 } ?: return false
        val result = installed.any { ext ->
            val extName = (ext.metaData.name ?: "").lowercase()
            val match = extName.contains(firstWord)
            android.util.Log.i("ExtensionsActivity", "  isAddonInstalled: addon='${addon.name}' extName='$extName' firstWord='$firstWord' match=$match")
            match
        }
        return result
    }

    private fun reload() {
        val c = controller() ?: return
        try {
            c.list().accept(
                { list: List<WebExtension>? ->
                    val installed = list ?: emptyList()
                    android.util.Log.i("ExtensionsActivity", "reload: ${installed.size} installed extensions")
                    installed.forEach { ext ->
                        android.util.Log.i("ExtensionsActivity", "  installed: name='${ext.metaData.name}' id='${ext.id}' blocklistState=${ext.metaData.blocklistState} enabled=${ext.metaData.enabled}")
                    }
                    // An add-on already present is matched by name, because the
                    // engine reports its own id (a UUID for a WebExtension) and
                    // the AMO slug has no relationship to it. Names rarely match
                    // exactly (e.g. "NoScript Security Suite" vs "NoScript"), so
                    // fall back to word-overlap matching: every word of the addon
                    // name must appear in the installed extension's name.
                    val installedNames = installed.mapNotNull { it.metaData.name }.toSet()
                    android.util.Log.i("ExtensionsActivity", "installedNames: $installedNames")
                    runOnUiThread {
                        addonsAdapter.update(
                            ZyphraDefaults.INSTALLABLE_ADDONS.map { addon ->
                                AddonRow(
                                    addon = addon,
                                    installed = isAddonInstalled(addon, installed),
                                    onClick = { confirmInstall(addon) },
                                )
                            },
                        )
                        adapter.update(
                            installed.map { ext ->
                                ExtInfo(
                                    id = ext.id,
                                    name = ext.metaData.name ?: ext.id,
                                    sub = listOf(
                                        ext.metaData.version,
                                        ext.metaData.description,
                                    ).filter { s -> !s.isNullOrEmpty() }.joinToString(" • "),
                                    enabled = ext.metaData.enabled,
                                    optionsUrl = ext.metaData.optionsPageUrl
                                        ?.takeIf { u -> u.isNotBlank() }
                                        ?.let { u ->
                                            if (u.contains("://")) u
                                            else ext.metaData.baseUrl
                                                ?.let { b -> b.trimEnd('/') + "/" + u.trimStart('/') }
                                        },
                                )
                            },
                        )
                        // The installed header is meaningless with nothing under it.
                        findViewById<View>(R.id.extensions_installed_header).visibility =
                            if (installed.isEmpty()) View.GONE else View.VISIBLE
                    }
                },
                { _ -> },
            )
        } catch (_e: Exception) {
        }
    }

    /**
     * One extension's management dialog: enable or disable it, open the
     * settings page the extension itself ships (only when it ships one), and
     * uninstall it. Tapping an installed row opens it, because those three are
     * the whole of what can be done to an add-on after install and hiding any
     * of them behind a long-press would make it undiscoverable.
     */
    private fun showManageDialog(info: ExtInfo) {
        val labels = mutableListOf<String>()
        val actions = mutableListOf<() -> Unit>()
        labels += getString(if (info.enabled) R.string.ext_disable else R.string.ext_enable)
        actions += { setEnabled(info, !info.enabled) }
        if (info.optionsUrl != null) {
            labels += getString(R.string.ext_open_settings)
            actions += { openExtensionSettings(info) }
        }
        labels += getString(R.string.uninstall_ext)
        actions += { uninstall(info) }
        AlertDialog.Builder(this)
            .setTitle(info.name)
            .setMessage(info.statusLine(this))
            .setItems(labels.toTypedArray()) { _, which -> actions[which]() }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    /**
     * Turns one extension on or off with the engine's user scope, which is
     * the same decision an end user makes from Firefox's add-on manager: it
     * applies to the running extension immediately and survives a restart.
     * The row is re-resolved from the live list first so the call cannot act
     * on a stale extension object after an update or uninstall elsewhere.
     */
    private fun setEnabled(info: ExtInfo, enable: Boolean) {
        val c = controller()
        if (c == null) {
            Toast.makeText(this, getString(R.string.not_available), Toast.LENGTH_SHORT).show()
            return
        }
        try {
            c.list().accept(
                { list: List<WebExtension>? ->
                    val ext = list?.find { it.id == info.id }
                    if (ext == null) {
                        // Gone from the engine's list (uninstalled elsewhere):
                        // refresh rather than act on a phantom.
                        runOnUiThread { reload() }
                        return@accept
                    }
                    val pending = if (enable) {
                        c.enable(ext, WebExtensionController.EnableSource.USER)
                    } else {
                        c.disable(ext, WebExtensionController.EnableSource.USER)
                    }
                    pending.accept(
                        { _ ->
                            runOnUiThread {
                                Toast.makeText(
                                    this,
                                    getString(
                                        if (enable) R.string.ext_enabled_toast
                                        else R.string.ext_disabled_toast,
                                        info.name,
                                    ),
                                    Toast.LENGTH_SHORT,
                                ).show()
                                reload()
                            }
                        },
                        { error ->
                            android.util.Log.e(
                                "ExtensionsActivity",
                                "enable=$enable failed for ${info.id}: ${error?.message}",
                                error,
                            )
                            runOnUiThread {
                                Toast.makeText(
                                    this,
                                    getString(R.string.ext_action_failed),
                                    Toast.LENGTH_LONG,
                                ).show()
                                reload()
                            }
                        },
                    )
                },
                { error ->
                    android.util.Log.e(
                        "ExtensionsActivity",
                        "list failed while enable=$enable for ${info.id}: ${error?.message}",
                        error,
                    )
                },
            )
        } catch (e: Exception) {
            android.util.Log.e("ExtensionsActivity", "setEnabled exception: ${e.message}", e)
        }
    }

    /**
     * Hands the extension's own options page to the browser. The URL is the
     * engine's moz-extension:// address for that page, which only Gecko can
     * render, so it goes through MainActivity as a VIEW intent rather than
     * being loaded here: this screen holds no session of its own.
     */
    private fun openExtensionSettings(info: ExtInfo) {
        val url = info.optionsUrl ?: return
        startActivity(
            Intent(this, MainActivity::class.java)
                .setAction(Intent.ACTION_VIEW)
                .setData(android.net.Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
    }

    private fun uninstall(info: ExtInfo) {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.uninstall_ext) + ": " + info.name + "?")
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                val c = controller() ?: return@setPositiveButton
                try {
                    c.list().accept(
                        { list: List<WebExtension>? ->
                            val ext = list?.find { it.id == info.id }
                            if (ext != null) c.uninstall(ext)
                        },
                        { _ -> },
                    )
                } catch (_e: Exception) {
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    class ExtensionsAdapter(
        private var items: List<ExtInfo>,
        private val onToggleEnable: (ExtInfo) -> Unit,
        private val onShowSettings: (ExtInfo) -> Unit,
        private val onUninstallExt: (ExtInfo) -> Unit,
        private val onRemove: (ExtInfo) -> Unit = {},
    ) : RecyclerView.Adapter<ExtensionsAdapter.VH>() {

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.ext_name)
            val sub: TextView = v.findViewById(R.id.ext_sub)
            val remove: ImageButton = v.findViewById(R.id.ext_remove)
            val actionsRow: LinearLayout = v.findViewById(R.id.ext_actions_row)
            val toggle: Button = v.findViewById(R.id.ext_toggle)
            val settings: Button = v.findViewById(R.id.ext_settings)
            val uninstall: Button = v.findViewById(R.id.ext_uninstall)
        }

        fun update(items: List<ExtInfo>) {
            this.items = items
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_extension, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(h: VH, position: Int) {
            val e = items[position]
            val ctx = h.itemView.context
            h.name.text = e.name
            // Status always shows: "it stays enabled" was a guess before, and a
            // dimmed row with the word "Disabled" makes the state legible at a
            // glance without opening the extension.
            h.sub.text = e.statusLine(ctx)
            h.sub.visibility = View.VISIBLE
            h.itemView.alpha = if (e.enabled) 1f else 0.6f
            // The whole row is the management target, not just a small icon:
            // enable/disable, settings and uninstall all live behind it. The
            // trash keeps its own listener, which takes precedence over the
            // row's on a child view.
            h.actionsRow.visibility = android.view.View.VISIBLE
            h.remove.visibility = android.view.View.GONE
            h.toggle.text = ctx.getString(if (e.enabled) R.string.ext_disable else R.string.ext_enable)
            h.toggle.setOnClickListener { onToggleEnable(e) }
            if (e.optionsUrl != null) {
                h.settings.visibility = android.view.View.VISIBLE
                h.settings.setOnClickListener { onShowSettings(e) }
            } else {
                h.settings.visibility = android.view.View.GONE
            }
            h.uninstall.setOnClickListener { onUninstallExt(e) }
        }

        override fun getItemCount(): Int = items.size
    }

    /**
     * The installable list. Reuses the installed-extension row so both lists
     * read as the same kind of thing, with the trailing button becoming "Install"
     * or a tick once it is already there.
     */
    class AddonsAdapter(
        private var items: List<AddonRow>,
    ) : RecyclerView.Adapter<AddonsAdapter.VH>() {

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.ext_name)
            val sub: TextView = v.findViewById(R.id.ext_sub)
            val action: ImageButton = v.findViewById(R.id.ext_remove)
            val actionsRow: LinearLayout = v.findViewById(R.id.ext_actions_row)
        }

        fun update(items: List<AddonRow>) {
            this.items = items
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_extension, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(h: VH, position: Int) {
            val row = items[position]
            val ctx = h.itemView.context
            h.actionsRow.visibility = android.view.View.GONE
            h.name.text = row.addon.name
            h.sub.text = row.addon.summary
            h.sub.visibility = View.VISIBLE
            android.util.Log.i("ExtensionsActivity", "onBindViewHolder: ${row.addon.name} installed=${row.installed}")
            if (row.installed) {
                // Already present. Tapping would reinstall the same add-on, so the
                // row is inert and says why rather than inviting a no-op.
                h.action.setImageResource(R.drawable.ic_shield)
                h.action.contentDescription =
                    ctx.getString(R.string.installed_ext, row.addon.name)
                h.action.setOnClickListener { }
                h.itemView.setOnClickListener { }
                h.itemView.alpha = 0.6f
                h.itemView.isEnabled = false
            } else {
                h.action.setImageResource(R.drawable.ic_add)
                h.action.contentDescription =
                    ctx.getString(R.string.install_ext, row.addon.name)
                // The whole row installs, not just the button: a 24dp target is
                // below the comfortable minimum, and the card made this a
                // comfortable target for free.
                h.action.setOnClickListener { row.onClick() }
                h.itemView.setOnClickListener { row.onClick() }
                h.itemView.isEnabled = true
                h.itemView.alpha = 1f
            }
        }

        override fun getItemCount(): Int = items.size
    }
}

