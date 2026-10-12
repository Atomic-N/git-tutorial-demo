<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/layout/header.jsp" %>
<section class="page-title-row">
    <div>
        <h1>审计日志</h1>
        <p class="muted">记录关键后台操作、关系操作和私信发送等行为。</p>
    </div>
    <a class="btn btn-ghost" href="${pageContext.request.contextPath}/admin/dashboard">返回后台</a>
</section>

<section class="report-list">
    <c:forEach var="log" items="${auditPage.items}">
        <article class="report-card">
            <div class="report-main">
                <div class="report-title">
                    <strong><c:out value="${log.action}"/></strong>
                    <span><c:out value="${log.targetType}"/></span>
                </div>
                <p>操作者：<c:out value="${empty log.actorName ? '系统' : log.actorName}"/> · ${log.createdAgo}</p>
                <p>目标 ID：${log.targetId}</p>
                <c:if test="${not empty log.detail}">
                    <p class="report-detail"><c:out value="${log.detail}"/></p>
                </c:if>
            </div>
        </article>
    </c:forEach>
    <c:if test="${empty auditPage.items}">
        <div class="empty-state">暂无审计日志。</div>
    </c:if>
</section>
<%@ include file="/WEB-INF/views/layout/footer.jsp" %>
