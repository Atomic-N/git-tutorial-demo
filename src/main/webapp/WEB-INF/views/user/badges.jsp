<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/layout/header.jsp" %>
<section class="profile-head compact">
    <img class="avatar" src="${profile.avatarUrl}" alt="">
    <div>
        <h1><c:out value="${profile.displayName}"/> 的徽章</h1>
        <p>@<c:out value="${profile.username}"/> · Lv${profile.trustLevel}</p>
    </div>
</section>

<nav class="profile-tabs">
    <a href="${pageContext.request.contextPath}/u/${profile.username}"><i data-lucide="list"></i>活动</a>
    <a class="active" href="${pageContext.request.contextPath}/u/${profile.username}/badges"><i data-lucide="award"></i>徽章</a>
    <c:if test="${not empty currentUser && (currentUser.id == profile.id || currentUser.admin)}">
        <a href="${pageContext.request.contextPath}/u/${profile.username}/preferences"><i data-lucide="settings"></i>偏好设置</a>
    </c:if>
</nav>

<section class="badge-panel">
    <div class="section-head">
        <h2>已获得</h2>
        <a href="${pageContext.request.contextPath}/badges">查看全部徽章</a>
    </div>
    <div class="badge-grid">
        <c:forEach var="badge" items="${badges}">
            <article class="badge-card earned" style="--badge-color:${badge.color}">
                <i data-lucide="${badge.icon}"></i>
                <div>
                    <strong><c:out value="${badge.name}"/></strong>
                    <p><c:out value="${badge.description}"/></p>
                    <span>${badge.awardedAgo}前获得</span>
                </div>
            </article>
        </c:forEach>
        <c:if test="${empty badges}">
            <div class="empty-state">还没有获得徽章。</div>
        </c:if>
    </div>
</section>

<section class="badge-panel subtle-panel">
    <div class="section-head">
        <h2>全部徽章进度</h2>
        <span>已点亮的徽章会显示获得时间</span>
    </div>
    <div class="badge-catalog compact-catalog">
        <c:forEach var="badge" items="${badgeCatalog}">
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
    </div>
</section>
<%@ include file="/WEB-INF/views/layout/footer.jsp" %>