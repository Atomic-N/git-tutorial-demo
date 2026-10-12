<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/layout/header.jsp" %>
<section class="page-title-row">
    <div>
        <h1>徽章</h1>
        <p class="muted">通过注册、发帖、回复、获赞和分享资料点亮社区徽章。</p>
    </div>
    <div class="admin-actions-inline">
        <c:if test="${not empty currentUser && currentUser.admin}">
            <a class="btn btn-primary" href="${pageContext.request.contextPath}/admin/badges"><i data-lucide="plus"></i>制作徽章</a>
        </c:if>
        <c:if test="${not empty currentUser}">
            <a class="btn btn-muted" href="${pageContext.request.contextPath}/u/${currentUser.username}/badges"><i data-lucide="award"></i>我的徽章</a>
        </c:if>
    </div>
</section>

<section class="badge-catalog">
    <c:forEach var="badge" items="${badges}">
        <article class="badge-card catalog ${badge.earned ? 'earned' : 'locked'}" style="--badge-color:${badge.color}">
            <i data-lucide="${badge.earned ? badge.icon : 'lock'}"></i>
            <div>
                <strong><c:out value="${badge.name}"/></strong>
                <p><c:out value="${badge.description}"/></p>
                <c:choose>
                    <c:when test="${badge.earned}"><span>${badge.awardedAgo}前获得</span></c:when>
                    <c:otherwise><span>尚未获得</span></c:otherwise>
                </c:choose>
            </div>
        </article>
    </c:forEach>
</section>
<%@ include file="/WEB-INF/views/layout/footer.jsp" %>
