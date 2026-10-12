<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/layout/header.jsp" %>
<section class="profile-head">
    <img class="avatar large" src="${profile.avatarUrl}" alt="">
    <div>
        <h1><c:out value="${profile.displayName}"/></h1>
        <div class="profile-role-row">
            <c:choose>
                <c:when test="${profile.admin}">
                    <span class="role-badge admin"><i data-lucide="shield-check"></i>管理员</span>
                </c:when>
                <c:when test="${profile.scopedModerator}">
                    <span class="role-badge moderator"><i data-lucide="shield"></i>版主</span>
                </c:when>
            </c:choose>
            <c:if test="${profile.scopedModerator && not empty profile.moderatedCategoryIds}">
                <c:forEach var="cat" items="${categories}">
                    <c:if test="${profile.moderatesCategory(cat.id)}">
                        <span class="category-badge" style="--cat-color:${cat.color}"><i data-lucide="${cat.icon}"></i><c:out value="${cat.name}"/></span>
                    </c:if>
                </c:forEach>
            </c:if>
        </div>
        <p>@<c:out value="${profile.username}"/> · Lv${profile.trustLevel} · 加入于 ${profile.joinedAgo}前</p>
        <c:if test="${not empty profile.bio}">
            <p class="profile-bio"><c:out value="${profile.bio}"/></p>
        </c:if>
    </div>
    <div class="profile-head-actions">
        <c:if test="${not empty currentUser && currentUser.id != profile.id}">
            <form class="inline-form" action="${pageContext.request.contextPath}/u/relation" method="post">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="username" value="${profile.username}">
                <input type="hidden" name="action" value="${relationshipState.following ? 'unfollow' : 'follow'}">
                <button class="btn btn-muted" type="submit"><i data-lucide="${relationshipState.following ? 'user-minus' : 'user-plus'}"></i>${relationshipState.following ? '取消关注' : '关注'}</button>
            </form>
            <form class="inline-form" action="${pageContext.request.contextPath}/u/relation" method="post">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="username" value="${profile.username}">
                <input type="hidden" name="action" value="${relationshipState.muted ? 'unmute' : 'mute'}">
                <button class="btn btn-ghost" type="submit"><i data-lucide="${relationshipState.muted ? 'volume-2' : 'volume-x'}"></i>${relationshipState.muted ? '取消静音' : '静音'}</button>
            </form>
            <form class="inline-form" action="${pageContext.request.contextPath}/u/relation" method="post">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="username" value="${profile.username}">
                <input type="hidden" name="action" value="${relationshipState.blocked ? 'unblock' : 'block'}">
                <button class="btn btn-danger" type="submit"><i data-lucide="shield-ban"></i>${relationshipState.blocked ? '取消拉黑' : '拉黑'}</button>
            </form>
            <c:if test="${allowPrivateMessages}">
                <a class="btn btn-primary" href="${pageContext.request.contextPath}/messages?to=${profile.username}"><i data-lucide="mail-plus"></i>发私信</a>
            </c:if>
        </c:if>
        <c:if test="${not empty currentUser && (currentUser.id == profile.id || currentUser.admin)}">
            <a class="btn btn-muted" href="${pageContext.request.contextPath}/u/${profile.username}/preferences"><i data-lucide="settings"></i>偏好设置</a>
        </c:if>
    </div>
</section>

<nav class="profile-tabs">
    <a class="active" href="${pageContext.request.contextPath}/u/${profile.username}"><i data-lucide="list"></i>活动</a>
    <a href="${pageContext.request.contextPath}/u/${profile.username}"><i data-lucide="message-square"></i>话题</a>
    <a href="${pageContext.request.contextPath}/u/${profile.username}/badges"><i data-lucide="award"></i>徽章</a>
    <a href="${pageContext.request.contextPath}/u/${profile.username}#following"><i data-lucide="users"></i>关注</a>
</nav>

<c:if test="${not empty param.error}">
    <div class="form-error"><c:out value="${param.error}"/></div>
</c:if>
<c:if test="${not empty param.message}">
    <div class="form-success"><c:out value="${param.message}"/></div>
</c:if>

<section class="profile-stats">
    <div><strong>${profile.topicsCount}</strong><span>话题</span></div>
    <div><strong>${profile.postsCount}</strong><span>回复</span></div>
    <div><strong>${profile.likesReceivedCount}</strong><span>获赞</span></div>
    <div><strong>${profile.trustLevel}</strong><span>等级</span></div>
</section>

<section id="badges" class="badge-panel">
    <div class="section-head">
        <h2>徽章</h2>
        <a href="${pageContext.request.contextPath}/u/${profile.username}/badges">查看全部</a>
    </div>
    <div class="badge-grid">
        <c:forEach var="badge" items="${badges}">
            <article class="badge-card" style="--badge-color:${badge.color}">
                <i data-lucide="${badge.icon}"></i>
                <div>
                    <strong><c:out value="${badge.name}"/></strong>
                    <p><c:out value="${badge.description}"/></p>
                    <span>${badge.awardedAgo}前获得</span>
                </div>
            </article>
        </c:forEach>
        <c:if test="${empty badges}">
            <div class="empty-state">继续发帖、回复和分享资料即可获得徽章。</div>
        </c:if>
    </div>
</section>

<section id="following" class="badge-panel">
    <div class="section-head">
        <h2>关注的人</h2>
    </div>
    <div class="badge-grid compact-catalog">
        <c:forEach var="followed" items="${followingUsers}">
            <a class="badge-card catalog" href="${pageContext.request.contextPath}/u/${followed.username}" style="--badge-color:#0ea5e9">
                <img class="avatar tiny" src="${followed.avatarUrl}" alt="">
                <div>
                    <strong><c:out value="${followed.displayName}"/></strong>
                    <p>@<c:out value="${followed.username}"/></p>
                    <span>Lv${followed.trustLevel}</span>
                </div>
            </a>
        </c:forEach>
        <c:if test="${empty followingUsers}">
            <div class="empty-state">还没有关注任何人。</div>
        </c:if>
    </div>
</section>

<section class="activity-list">
    <h2>最近话题</h2>
    <c:forEach var="topic" items="${topicsPage.items}">
        <article class="activity-item">
            <img class="avatar tiny" src="${topic.authorAvatarUrl}" alt="">
            <div>
                <a class="topic-title" href="${pageContext.request.contextPath}/t/${topic.id}"><c:out value="${topic.title}"/></a>
                <div class="topic-meta">
                    <span>${topic.createdAgo}</span>
                    <span>${topic.repliesCount} 回复</span>
                    <a class="category-badge" style="--cat-color:${topic.categoryColor}" href="${pageContext.request.contextPath}/c/${topic.categorySlug}"><c:out value="${topic.categoryName}"/></a>
                </div>
            </div>
        </article>
    </c:forEach>
    <c:if test="${empty topicsPage.items}">
        <div class="empty-state">这个用户还没有发布话题。</div>
    </c:if>
</section>
<%@ include file="/WEB-INF/views/layout/footer.jsp" %>
