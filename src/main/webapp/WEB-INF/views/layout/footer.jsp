    </main>
</div>
<script>
(() => {
  const applySidebarState = (collapsed) => {
    const shell = document.querySelector(".app-shell");
    const sidebar = document.getElementById("appSidebar");
    const toggle = document.querySelector(".sidebar-toggle");
    const content = document.querySelector(".content");
    if (!shell || !toggle) return;

    shell.classList.toggle("sidebar-collapsed", collapsed);
    shell.style.gridTemplateColumns = collapsed ? "0 minmax(0, 1fr)" : "";
    toggle.setAttribute("aria-expanded", collapsed ? "false" : "true");
    toggle.setAttribute("aria-label", collapsed ? "展开侧边栏" : "收起侧边栏");

    if (sidebar) {
      sidebar.toggleAttribute("hidden", collapsed);
      sidebar.setAttribute("aria-hidden", collapsed ? "true" : "false");
      sidebar.style.display = collapsed ? "none" : "";
      sidebar.style.width = collapsed ? "0" : "";
      sidebar.style.maxWidth = collapsed ? "0" : "";
      sidebar.style.minWidth = collapsed ? "0" : "";
      sidebar.style.padding = collapsed ? "0" : "";
      sidebar.style.borderRightWidth = collapsed ? "0" : "";
      sidebar.style.overflow = collapsed ? "hidden" : "";
    }

    if (content) {
      content.style.gridColumn = collapsed ? "1 / -1" : "";
    }
  };

  const setupSidebarFallback = () => {
    const toggle = document.querySelector(".sidebar-toggle");
    const shell = document.querySelector(".app-shell");
    if (!toggle || !shell || toggle.dataset.sidebarBound === "true") return;

    const storageKey = "ai-tef-sidebar-collapsed";
    applySidebarState(window.localStorage.getItem(storageKey) === "true");
    toggle.dataset.sidebarBound = "true";
    toggle.addEventListener("click", () => {
      const collapsed = !shell.classList.contains("sidebar-collapsed");
      applySidebarState(collapsed);
      window.localStorage.setItem(storageKey, collapsed ? "true" : "false");
    });
  };

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", setupSidebarFallback, { once: true });
  } else {
    setupSidebarFallback();
  }
})();
</script>
</body>
</html>
