document.addEventListener("DOMContentLoaded", () => {
  if (window.lucide) {
    window.lucide.createIcons();
  }

  setupSidebarToggle();
  setupAvatarFallbacks();

  document.querySelectorAll(".ajax-toggle").forEach((button) => {
    button.addEventListener("click", async () => {
      const action = button.dataset.action;
      const id = button.dataset.id;
      if (!action || !id || button.disabled) return;
      button.disabled = true;
      try {
        const body = new URLSearchParams();
        body.set("id", id);
        body.set("csrfToken", window.CSRF_TOKEN || "");
        const response = await axios.post(`${window.APP_CONTEXT || ""}/api/interactions/${action}`, body);
        const payload = response.data;
        if (payload.code !== 0) {
          window.location.href = `${window.APP_CONTEXT || ""}/login`;
          return;
        }
        updateInteractionCounts(button, action, id, payload.data);
        if (window.lucide) {
          window.lucide.createIcons();
        }
      } catch (error) {
        console.error(error);
      } finally {
        button.disabled = false;
      }
    });
  });

  document.querySelectorAll(".quote-button").forEach((button) => {
    button.addEventListener("click", () => {
      const input = document.getElementById("replyToPostId");
      const textarea = document.querySelector("#reply-box textarea");
      const banner = document.getElementById("replyTargetBanner");
      const replyTargetName = document.getElementById("replyTargetName");
      if (input) input.value = button.dataset.post || "";
      if (banner && replyTargetName) {
        replyTargetName.textContent = button.dataset.author || "";
        banner.hidden = !button.dataset.author;
      }
      if (textarea) {
        textarea.focus();
        textarea.scrollIntoView({ behavior: "smooth", block: "center" });
      }
    });
  });

  const clearReplyButton = document.getElementById("replyTargetClear");
  if (clearReplyButton) {
    clearReplyButton.addEventListener("click", () => {
      const input = document.getElementById("replyToPostId");
      const banner = document.getElementById("replyTargetBanner");
      const replyTargetName = document.getElementById("replyTargetName");
      if (input) input.value = "";
      if (replyTargetName) replyTargetName.textContent = "";
      if (banner) banner.hidden = true;
    });
  }

  setupDraftForms();
  setupUploads();
  setupAdminTools();
  });

function updateInteractionCounts(sourceButton, action, id, data) {
  const active = Boolean(data.active);
  const count = data.count;
  const syncKey = sourceButton.dataset.syncKey || `${action}:${id}`;

  document.querySelectorAll(".ajax-toggle").forEach((button) => {
    const sameTarget = button.dataset.action === action && button.dataset.id === id;
    const sameSyncKey = button.dataset.syncKey && button.dataset.syncKey === syncKey;
    if (!sameTarget && !sameSyncKey) return;

    button.classList.toggle("active", active);
    button.querySelectorAll("[data-count]").forEach((countElement) => {
      countElement.textContent = count;
    });
  });

  document.querySelectorAll("[data-sync-count]").forEach((countElement) => {
    if (countElement.dataset.syncCount === syncKey) {
      countElement.textContent = count;
    }
  });
}

function setupAvatarFallbacks() {
  const fallback = window.DEFAULT_AVATAR_URL || `${window.APP_CONTEXT || ""}/static/images/default-avatar.svg`;
  const selector = [
    "img.avatar",
    ".avatar-mini img",
    ".avatar-link img",
    ".admin-user-card img",
    ".badge-card img"
  ].join(",");

  document.querySelectorAll(selector).forEach((img) => {
    const applyFallback = () => {
      if (img.dataset.avatarFallbackApplied === "true") return;
      img.dataset.avatarFallbackApplied = "true";
      img.src = fallback;
    };

    img.addEventListener("error", applyFallback);
    if (!img.getAttribute("src")) {
      applyFallback();
    }
  });
}

function setupSidebarToggle() {
  const shell = document.querySelector(".app-shell");
  const toggle = document.querySelector(".sidebar-toggle");
  const sidebar = document.getElementById("appSidebar");
  const content = document.querySelector(".content");
  if (!shell || !toggle) return;

  const storageKey = "ai-tef-sidebar-collapsed";
  const applyState = (collapsed) => {
    shell.classList.toggle("sidebar-collapsed", collapsed);
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
    shell.style.gridTemplateColumns = collapsed ? "0 minmax(0, 1fr)" : "";
    if (content) {
      content.style.gridColumn = collapsed ? "1 / -1" : "";
    }
    toggle.setAttribute("aria-expanded", collapsed ? "false" : "true");
    toggle.setAttribute("aria-label", collapsed ? "\u5c55\u5f00\u4fa7\u8fb9\u680f" : "\u6536\u8d77\u4fa7\u8fb9\u680f");
  };

  applyState(window.localStorage.getItem(storageKey) === "true");
  toggle.addEventListener("click", () => {
    const collapsed = !shell.classList.contains("sidebar-collapsed");
    applyState(collapsed);
    window.localStorage.setItem(storageKey, collapsed ? "true" : "false");
  });
}

function setupDraftForms() {
  if (window.AUTO_SAVE_DRAFTS === false || !window.axios) return;
  document.querySelectorAll(".draft-form").forEach((form) => {
    const type = form.dataset.draftType;
    const targetId = form.dataset.draftTarget || "";
    const titleInput = form.querySelector("[data-draft-title]");
    const contentInput = form.querySelector("[data-draft-content]");
    const status = form.querySelector("[data-draft-status]");
    if (!type || !contentInput) return;

    const setStatus = (text) => {
      if (status) status.textContent = text;
    };

    loadDraft(type, targetId, titleInput, contentInput, setStatus);

    let timer = null;
    const scheduleSave = () => {
      clearTimeout(timer);
      timer = setTimeout(() => saveDraft(type, targetId, titleInput, contentInput, setStatus), 900);
    };
    [titleInput, contentInput].filter(Boolean).forEach((field) => field.addEventListener("input", scheduleSave));
    form.addEventListener("submit", () => setStatus("\u6b63\u5728\u53d1\u5e03..."));
  });
}

async function loadDraft(type, targetId, titleInput, contentInput, setStatus) {
  try {
    const params = new URLSearchParams({ type });
    if (targetId) params.set("targetId", targetId);
    const response = await axios.get(`${window.APP_CONTEXT || ""}/api/drafts?${params.toString()}`);
    const payload = response.data;
    if (payload.code !== 0 || !payload.data.exists) return;
    if (titleInput && !titleInput.value) titleInput.value = payload.data.title || "";
    if (contentInput && !contentInput.value) contentInput.value = payload.data.content || "";
    setStatus(`\u5df2\u6062\u590d ${payload.data.updatedAgo}\u524d\u7684\u8349\u7a3f`);
  } catch (error) {
    console.debug("draft load skipped", error);
  }
}

async function saveDraft(type, targetId, titleInput, contentInput, setStatus) {
  const title = titleInput ? titleInput.value : "";
  const content = contentInput ? contentInput.value : "";
  if (!title.trim() && !content.trim()) return;
  try {
    const body = new URLSearchParams();
    body.set("type", type);
    body.set("csrfToken", window.CSRF_TOKEN || "");
    if (targetId) body.set("targetId", targetId);
    body.set("title", title);
    body.set("content", content);
    const response = await axios.post(`${window.APP_CONTEXT || ""}/api/drafts`, body);
    const payload = response.data;
    setStatus(payload.code === 0 ? "\u8349\u7a3f\u5df2\u4fdd\u5b58" : payload.message || "\u8349\u7a3f\u4fdd\u5b58\u5931\u8d25");
  } catch (error) {
    setStatus("\u8349\u7a3f\u4fdd\u5b58\u5931\u8d25");
  }
}

function setupUploads() {
  if (!window.axios) return;
  document.querySelectorAll(".file-upload-input").forEach((input) => {
    input.addEventListener("change", async () => {
      const file = input.files && input.files[0];
      if (!file) return;
      const container = input.closest(".editor-tools") || input.parentElement;
      const status = container ? container.querySelector("[data-upload-status]") : null;
      const targetSelector = input.dataset.insertTarget;
      const insertMode = input.dataset.insertMode || "append";
      const target = targetSelector ? document.querySelector(targetSelector) : null;
      if (status) status.textContent = "\u4e0a\u4f20\u4e2d...";
      input.disabled = true;
      try {
        const body = new FormData();
        body.append("file", file);
        const response = await axios.post(`${window.APP_CONTEXT || ""}/api/uploads`, body, {
          headers: {
            "Content-Type": "multipart/form-data",
            "X-CSRF-Token": window.CSRF_TOKEN || ""
          }
        });
        const payload = response.data;
        if (payload.code !== 0) {
          if (status) status.textContent = payload.message || "\u4e0a\u4f20\u5931\u8d25";
          return;
        }
        const url = payload.data.url;
        if (target) {
          if (insertMode === "value") {
            target.value = url;
          } else {
            const prefix = target.value && !target.value.endsWith("\n") ? "\n" : "";
            const isImage = typeof file.type === "string" && file.type.startsWith("image/");
            const inserted = isImage
              ? `![${payload.data.originalName}](${url})`
              : `[${payload.data.originalName}](${url})`;
            target.value += `${prefix}${inserted}\n`;
            target.dispatchEvent(new Event("input", { bubbles: true }));
          }
        }
        if (status) status.textContent = "\u4e0a\u4f20\u5b8c\u6210";
      } catch (error) {
        if (status) status.textContent = "\u4e0a\u4f20\u5931\u8d25";
      } finally {
        input.value = "";
        input.disabled = false;
      }
    });
  });
}

function setupAdminTools() {
  document.querySelectorAll("[data-admin-select-all]").forEach((selectAll) => {
    const group = selectAll.dataset.adminSelectAll;
    const items = Array.from(document.querySelectorAll(`[data-admin-select-item="${group}"]`));
    const submitButtons = Array.from(document.querySelectorAll("[data-admin-bulk-submit]"));
    if (!group || items.length === 0) return;

    const sync = () => {
      const checkedCount = items.filter((item) => item.checked).length;
      selectAll.checked = checkedCount > 0 && checkedCount === items.length;
      selectAll.indeterminate = checkedCount > 0 && checkedCount < items.length;
      submitButtons.forEach((button) => {
        button.disabled = checkedCount === 0;
      });
    };

    selectAll.addEventListener("change", () => {
      items.forEach((item) => {
        item.checked = selectAll.checked;
      });
      sync();
    });
    items.forEach((item) => item.addEventListener("change", sync));
    sync();
  });

  const bulkTopicsForm = document.getElementById("bulkTopicsForm");
  if (bulkTopicsForm) {
    bulkTopicsForm.addEventListener("submit", (event) => {
      const checked = document.querySelectorAll('[data-admin-select-item="topicIds"]:checked');
      if (checked.length === 0) {
        event.preventDefault();
        return;
      }
      const action = document.querySelector('[name="bulkAction"][form="bulkTopicsForm"]');
      if (action && action.value === "delete" && !window.confirm(`确认删除选中的 ${checked.length} 个话题？`)) {
        event.preventDefault();
      }
    });
  }
}
