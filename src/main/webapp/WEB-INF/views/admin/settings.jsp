<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/layout/header.jsp" %>
<section class="page-title-row">
    <div>
        <h1>站点配置</h1>
        <p class="muted">集中维护站点名称、公告和功能开关。</p>
    </div>
    <a class="btn btn-ghost" href="${pageContext.request.contextPath}/admin/dashboard">返回后台</a>
</section>

<c:if test="${not empty param.message}">
    <div class="form-success"><c:out value="${param.message}"/></div>
</c:if>

<form class="settings-form admin-settings-form" action="${pageContext.request.contextPath}/admin/settings" method="post">
    <input type="hidden" name="csrfToken" value="${csrfToken}">
    <label>站点名称
        <input name="siteName" value="${settingsMap.site_name}" required>
    </label>
    <label>顶部公告
        <textarea name="noticeText" rows="4">${settingsMap.notice_text}</textarea>
    </label>
    <label>社区准则链接
        <input name="communityGuidelinesUrl" value="${settingsMap.community_guidelines_url}" required>
    </label>
    <label class="switch-row"><input type="checkbox" name="allowPrivateMessages" ${settingsMap.allow_private_messages == 'true' ? 'checked' : ''}>开启私信功能</label>
    <label class="switch-row"><input type="checkbox" name="markdownEnabled" ${settingsMap.markdown_enabled == 'true' ? 'checked' : ''}>启用 Markdown 渲染</label>
    <button class="btn btn-primary" type="submit">保存配置</button>
</form>
<%@ include file="/WEB-INF/views/layout/footer.jsp" %>
