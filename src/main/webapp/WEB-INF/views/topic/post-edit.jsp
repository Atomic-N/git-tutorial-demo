<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/layout/header.jsp" %>
<section class="editor-page">
    <h1>编辑回复</h1>
    <p class="muted">修改后会直接更新原回复内容。</p>
    <c:if test="${not empty error}">
        <div class="form-error"><c:out value="${error}"/></div>
    </c:if>
    <form class="topic-editor" action="${pageContext.request.contextPath}/posts/edit" method="post">
        <input type="hidden" name="csrfToken" value="${csrfToken}">
        <input type="hidden" name="postId" value="${editablePost.id}">
        <input type="hidden" name="topicId" value="${editablePost.topicId}">
        <label>回复内容
            <textarea name="content" rows="12" placeholder="完善这条回复的内容" required>${formContentValue}</textarea>
        </label>
        <div class="editor-tools">
            <label class="upload-tool"><i data-lucide="paperclip"></i>上传附件
                <input class="file-upload-input" type="file" data-insert-target="textarea[name='content']">
            </label>
            <span class="upload-status" data-upload-status></span>
        </div>
        <div class="form-actions">
            <button class="btn btn-primary" type="submit"><i data-lucide="save"></i>保存修改</button>
            <a class="btn btn-ghost" href="${cancelUrl}">取消</a>
        </div>
    </form>
</section>
<%@ include file="/WEB-INF/views/layout/footer.jsp" %>
