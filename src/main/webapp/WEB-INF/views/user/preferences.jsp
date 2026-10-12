<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/layout/header.jsp" %>
<section class="profile-head compact">
    <img class="avatar" src="${profile.avatarUrl}" alt="">
    <div>
        <h1><c:out value="${profile.displayName}"/></h1>
        <p>@<c:out value="${profile.username}"/></p>
    </div>
</section>

<nav class="profile-tabs">
    <a href="${pageContext.request.contextPath}/u/${profile.username}"><i data-lucide="user"></i>总结</a>
    <a class="active" href="${pageContext.request.contextPath}/u/${profile.username}/preferences"><i data-lucide="settings"></i>偏好设置</a>
</nav>

<c:if test="${not empty param.message}">
    <div class="form-success"><c:out value="${param.message}"/></div>
</c:if>

<section class="preferences">
    <aside class="settings-nav">
        <a class="active" href="#account"><i data-lucide="circle-user-round"></i>账户</a>
        <a href="#profile"><i data-lucide="id-card"></i>个人资料</a>
        <a href="#notice"><i data-lucide="bell"></i>通知</a>
        <a href="#interface"><i data-lucide="monitor"></i>界面</a>
        <a href="#uploads"><i data-lucide="paperclip"></i>文件</a>
    </aside>
    <form class="settings-form" action="${pageContext.request.contextPath}/u/${profile.username}/preferences" method="post">
        <input type="hidden" name="csrfToken" value="${csrfToken}">
        <section id="account" class="settings-section">
            <h2>账户</h2>
            <c:if test="${not empty error}">
                <div class="form-error"><c:out value="${error}"/></div>
            </c:if>
            <label>用户名
                <input value="${profile.username}" disabled>
            </label>
            <label>昵称
                <input name="displayName" value="${profile.displayName}" required>
            </label>
        </section>

        <section id="profile" class="settings-section">
            <h2>个人资料</h2>
            <label>头像 URL
                <input id="avatarUrl" name="avatarUrl" value="${profile.avatarUrl}">
            </label>
            <div class="editor-tools compact-tools">
                <label class="upload-tool"><i data-lucide="image-up"></i>上传头像
                    <input class="file-upload-input" type="file" accept="image/*" data-insert-target="#avatarUrl" data-insert-mode="value">
                </label>
                <span class="upload-status" data-upload-status></span>
            </div>
            <label>位置
                <input name="location" value="${profile.location}">
            </label>
            <label>个人网站
                <input name="website" value="${profile.website}">
            </label>
            <label>个人简介
                <textarea name="bio" rows="5">${profile.bio}</textarea>
            </label>
        </section>

        <section id="notice" class="settings-section setting-switches">
            <h2>通知</h2>
            <label class="switch-row"><input type="checkbox" name="notifyOnReply" ${preference.notifyOnReply ? 'checked' : ''}>话题收到回复时通知我</label>
            <label class="switch-row"><input type="checkbox" name="notifyOnLike" ${preference.notifyOnLike ? 'checked' : ''}>内容收到赞时通知我</label>
            <label class="switch-row"><input type="checkbox" name="emailOnMention" ${preference.emailOnMention ? 'checked' : ''}>被提及时允许邮件提醒</label>
            <label class="switch-row"><input type="checkbox" name="emailDigest" ${preference.emailDigest ? 'checked' : ''}>接收社区摘要邮件</label>
        </section>

        <section id="interface" class="settings-section setting-grid">
            <h2>界面与编辑器</h2>
            <label>主题
                <select name="theme">
                    <option value="SYSTEM" ${preference.theme == 'SYSTEM' ? 'selected' : ''}>跟随系统</option>
                    <option value="LIGHT" ${preference.theme == 'LIGHT' ? 'selected' : ''}>浅色</option>
                    <option value="DARK" ${preference.theme == 'DARK' ? 'selected' : ''}>深色</option>
                </select>
            </label>
            <label>编辑器模式
                <select name="editorMode">
                    <option value="RICH" ${preference.editorMode == 'RICH' ? 'selected' : ''}>增强文本</option>
                    <option value="MARKDOWN" ${preference.editorMode == 'MARKDOWN' ? 'selected' : ''}>Markdown</option>
                    <option value="PLAIN" ${preference.editorMode == 'PLAIN' ? 'selected' : ''}>纯文本</option>
                </select>
            </label>
            <label class="switch-row"><input type="checkbox" name="autoSaveDrafts" ${preference.autoSaveDrafts ? 'checked' : ''}>自动保存草稿</label>
            <label class="switch-row"><input type="checkbox" name="hideReadTopics" ${preference.hideReadTopics ? 'checked' : ''}>浏览列表时减少已读话题提示</label>
        </section>

        <section id="uploads" class="settings-section">
            <h2>最近上传</h2>
            <div class="upload-list">
                <c:forEach var="file" items="${uploads}">
                    <a class="upload-item" href="${pageContext.request.contextPath}${file.publicUrl}" target="_blank">
                        <i data-lucide="file"></i>
                        <span><c:out value="${file.originalName}"/></span>
                        <small>${file.createdAgo}前</small>
                    </a>
                </c:forEach>
                <c:if test="${empty uploads}">
                    <div class="empty-state">还没有上传过文件。</div>
                </c:if>
            </div>
        </section>

        <button class="btn btn-primary" type="submit">保存设置</button>
    </form>
</section>
<%@ include file="/WEB-INF/views/layout/footer.jsp" %>
