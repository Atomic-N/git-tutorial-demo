<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/layout/header.jsp" %>
<section class="page-title-row">
    <div>
        <h1>通知</h1>
        <p class="muted">回复、点赞和系统消息会集中显示在这里。</p>
    </div>
    <form action="${pageContext.request.contextPath}/notifications" method="post">
        <input type="hidden" name="csrfToken" value="${csrfToken}">
        <button class="btn btn-muted" type="submit"><i data-lucide="check-check"></i>全部已读</button>
    </form>
</section>

<section class="notification-list">
    <c:forEach var="notice" items="${notificationsPage.items}">
        <a class="notification-item ${notice.read ? '' : 'unread'}" href="${pageContext.request.contextPath}${notice.targetUrl}">
            <c:choose>
                <c:when test="${empty notice.actorAvatarUrl}">
                    <img class="avatar tiny" src="${pageContext.request.contextPath}/static/images/default-avatar.svg" alt="">
                </c:when>
                <c:otherwise>
                    <img class="avatar tiny" src="${notice.actorAvatarUrl}" alt="">
                </c:otherwise>
            </c:choose>
            <div>
                <strong><c:out value="${notice.title}"/></strong>
                <p><c:out value="${notice.body}"/></p>
            </div>
            <span>${notice.createdAgo}</span>
        </a>
    </c:forEach>
    <c:if test="${empty notificationsPage.items}">
        <div class="empty-state">暂无通知。</div>
    </c:if>
</section>
<%@ include file="/WEB-INF/views/layout/footer.jsp" %>
