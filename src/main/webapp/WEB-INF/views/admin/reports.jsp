<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/layout/header.jsp" %>
<section class="page-title-row">
    <div>
        <h1>举报处理</h1>
        <p class="muted">审核用户提交的内容举报，并标记处理结果。</p>
    </div>
    <a class="btn btn-ghost" href="${pageContext.request.contextPath}/admin/dashboard">返回后台</a>
</section>

<c:if test="${not empty param.error}">
    <div class="form-error"><c:out value="${param.error}"/></div>
</c:if>
<c:if test="${not empty param.message}">
    <div class="form-success"><c:out value="${param.message}"/></div>
</c:if>

<nav class="tabs admin-tabs">
    <a class="${status == 'PENDING' ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/reports?status=PENDING">待处理</a>
    <a class="${status == 'RESOLVED' ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/reports?status=RESOLVED">已处理</a>
    <a class="${status == 'REJECTED' ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/reports?status=REJECTED">已驳回</a>
    <a class="${status == 'ALL' ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/reports?status=ALL">全部</a>
</nav>

<section class="report-list">
    <c:forEach var="report" items="${reportsPage.items}">
        <article class="report-card">
            <div class="report-main">
                <div class="report-title">
                    <strong><c:out value="${report.reason}"/></strong>
                    <span>${report.status}</span>
                </div>
                <p>目标：<a href="${pageContext.request.contextPath}${report.targetUrl}"><c:out value="${report.targetTitle}"/></a></p>
                <p>举报人：<c:out value="${report.reporterName}"/> · ${report.createdAgo}</p>
                <c:if test="${not empty report.detail}">
                    <p class="report-detail"><c:out value="${report.detail}"/></p>
                </c:if>
            </div>
            <form class="report-actions" action="${pageContext.request.contextPath}/admin/reports" method="post">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="reportId" value="${report.id}">
                <button class="btn btn-muted" name="status" value="RESOLVED" type="submit">标记已处理</button>
                <button class="btn btn-ghost" name="status" value="REJECTED" type="submit">驳回</button>
                <button class="btn btn-danger" name="action" value="resolve-hide" type="submit" onclick="return confirm('确认隐藏被举报内容？')">处理并隐藏</button>
            </form>
        </article>
    </c:forEach>
    <c:if test="${empty reportsPage.items}">
        <div class="empty-state">暂无举报。</div>
    </c:if>
</section>
<%@ include file="/WEB-INF/views/layout/footer.jsp" %>
