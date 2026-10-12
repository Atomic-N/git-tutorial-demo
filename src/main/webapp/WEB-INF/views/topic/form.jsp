<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/layout/header.jsp" %>
<section class="editor-page">
    <h1>${editingTopic ? '编辑话题' : '新建话题'}</h1>
    <c:if test="${not empty error}">
        <div class="form-error"><c:out value="${error}"/></div>
    </c:if>
    <c:if test="${not empty draft && !editingTopic}">
        <div class="form-success draft-restore"><i data-lucide="file-clock"></i>已恢复 ${draft.updatedAgo}前保存的草稿。</div>
    </c:if>
    <form class="topic-editor ${editingTopic ? '' : 'draft-form'}" action="${pageContext.request.contextPath}${editingTopic ? '/topics/edit' : '/topics/create'}" method="post" data-draft-type="TOPIC" data-draft-target="">
        <input type="hidden" name="csrfToken" value="${csrfToken}">
        <c:if test="${editingTopic}">
            <input type="hidden" name="topicId" value="${topicEditId}">
        </c:if>
        <label>标题
            <input name="title" data-draft-title maxlength="180" placeholder="清楚描述你想讨论的问题或分享" value="${not empty formTitleValue ? formTitleValue : (not empty param.title ? param.title : (not empty draft ? draft.title : ''))}" required>
        </label>
        <label>分类
            <select name="categoryId" required>
                <option value="">选择分类...</option>
                <c:forEach var="cat" items="${categories}">
                    <option value="${cat.id}" ${(not empty formCategoryIdValue ? formCategoryIdValue : param.categoryId) == cat.id ? 'selected' : ''}><c:out value="${cat.name}"/></option>
                </c:forEach>
            </select>
        </label>
        <label>标签
            <input name="tags" placeholder="例如：Agent RAG 软件开发，最多 5 个" value="${not empty formTagsValue ? formTagsValue : param.tags}">
        </label>
        <label>内容
            <textarea name="content" data-draft-content rows="14" placeholder="分享背景、尝试过的方法、代码片段或结论" required>${not empty formContentValue ? formContentValue : (not empty param.content ? param.content : (not empty draft ? draft.content : ''))}</textarea>
        </label>
        <div class="editor-tools">
            <label class="upload-tool"><i data-lucide="paperclip"></i>上传附件
                <input class="file-upload-input" type="file" data-insert-target="textarea[name='content']">
            </label>
            <span class="upload-status" data-upload-status></span>
            <span class="draft-status" data-draft-status>${editingTopic ? '修改后将直接覆盖当前内容' : '草稿会自动保存'}</span>
        </div>
        <div class="form-actions">
            <button class="btn btn-primary" type="submit"><i data-lucide="${editingTopic ? 'save' : 'send'}"></i>${editingTopic ? '保存修改' : '发布话题'}</button>
            <a class="btn btn-ghost" href="${not empty cancelUrl ? cancelUrl : pageContext.request.contextPath.concat('/latest')}">取消</a>
        </div>
    </form>
</section>
<%@ include file="/WEB-INF/views/layout/footer.jsp" %>
