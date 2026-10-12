<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/layout/header.jsp" %>
<section class="page-title-row">
    <div>
        <h1>私信</h1>
        <p class="muted">与社区成员一对一交流，适合私下沟通和跟进协作。</p>
    </div>
</section>

<c:if test="${not empty param.error}">
    <div class="form-error"><c:out value="${param.error}"/></div>
</c:if>

<section class="chat-layout message-layout">
    <aside class="chat-sidebar">
        <button class="btn btn-primary btn-block" type="button" onclick="document.getElementById('new-message-modal').style.display='flex'">
            <i data-lucide="edit-3"></i> 新建私信
        </button>

        <div class="chat-search">
            <input type="text" id="chat-search-input" placeholder="搜索会话...">
            <i data-lucide="search"></i>
        </div>

        <div id="chat-channels-list">
            <c:forEach var="item" items="${threads}">
                <a class="chat-channel ${thread != null && thread.id == item.id ? 'active' : ''}"
                   href="${pageContext.request.contextPath}/messages/${item.id}"
                   data-thread-id="${item.id}"
                   data-username="${item.partnerUsername}"
                   data-displayname="${item.partnerDisplayName}"
                   data-preview="${item.latestPreview}">
                    <img class="avatar" src="${item.partnerAvatarUrl}" alt="${item.partnerDisplayName}">
                    <div class="chat-channel-info">
                        <strong><c:out value="${item.partnerDisplayName}"/></strong>
                        <p><c:out value="${item.latestPreview}"/></p>
                    </div>
                    <div class="chat-channel-meta">
                        <button class="chat-channel-menu-btn" type="button" onclick="showThreadMenu(event, ${item.id})">
                            <i data-lucide="more-vertical"></i>
                        </button>
                        <c:if test="${item.unreadCount > 0}">
                            <span class="chat-channel-badge">${item.unreadCount}</span>
                        </c:if>
                        <time class="chat-channel-time">${item.latestAgo}</time>
                    </div>
                </a>
            </c:forEach>
        </div>

        <c:if test="${empty threads}">
            <div class="empty-state">还没有私信会话。</div>
        </c:if>
    </aside>

    <div class="chat-panel">
        <c:choose>
            <c:when test="${not empty thread}">
                <div class="chat-panel-head">
                    <div>
                        <h2><c:out value="${thread.partnerDisplayName}"/></h2>
                        <p class="muted">@<c:out value="${thread.partnerUsername}"/></p>
                    </div>
                    <img class="avatar" src="${thread.partnerAvatarUrl}" alt="">
                </div>
                <div class="chat-stream">
                    <c:forEach var="message" items="${messages}">
                        <article class="chat-message" data-message-id="${message.id}">
                            <img class="avatar" src="${message.senderAvatarUrl}" alt="">
                            <div>
                                <div class="chat-meta">
                                    <strong><c:out value="${message.senderDisplayName}"/></strong>
                                    <span>@<c:out value="${message.senderUsername}"/></span>
                                    <span>${message.createdAgo}</span>
                                    <c:if test="${message.senderId == sessionScope.currentUser.id && not empty message.statusIcon}">
                                        <span class="message-status ${message.read ? 'read' : ''}" title="${message.read ? '已读' : '已发送'}">
                                            ${message.statusIcon}
                                        </span>
                                    </c:if>
                                    <button class="message-action-btn" type="button"
                                            onclick="quoteMessage(${message.id}, '${fn:escapeXml(message.senderDisplayName)}', '${fn:escapeXml(message.bodyRaw)}')"
                                            title="引用回复">
                                        <i data-lucide="corner-up-left"></i>
                                    </button>
                                </div>
                                <c:if test="${not empty message.quotedMessageBody}">
                                    <div class="chat-reply-ref">
                                        <strong>${message.quotedSenderName}:</strong>
                                        <c:out value="${message.quotedMessageBody}"/>
                                    </div>
                                </c:if>
                                <div class="cooked-content compact">${message.bodyCooked}</div>
                            </div>
                        </article>
                    </c:forEach>
                </div>
                <form class="chat-form" action="${pageContext.request.contextPath}/messages" method="post">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="username" value="${thread.partnerUsername}">
                    <input type="hidden" name="quotedMessageId" id="quotedMessageId" value="">

                    <div id="quoted-preview" class="quoted-message-preview" style="display: none;">
                        <div class="quoted-preview-header">
                            <span>引用 <strong id="quoted-sender-name"></strong> 的消息:</span>
                            <button type="button" onclick="clearQuote()">&times;</button>
                        </div>
                        <div class="quoted-preview-content" id="quoted-content"></div>
                    </div>

                    <textarea name="content" rows="3" placeholder="回复 ${thread.partnerDisplayName}..." required></textarea>
                    <div class="chat-form-actions">
                        <button class="btn btn-primary" type="submit">
                            <i data-lucide="send"></i> 发送
                        </button>
                    </div>
                </form>
            </c:when>
            <c:otherwise>
                <div class="empty-state standalone">选择一个会话，或者点击左上角新建私信。</div>
            </c:otherwise>
        </c:choose>
    </div>
</section>

<!-- 新建私信模态框 -->
<div class="modal" id="new-message-modal" style="display: none;">
    <div class="modal-dialog">
        <div class="modal-header">
            <h3>发送私信</h3>
            <button type="button" class="modal-close" onclick="document.getElementById('new-message-modal').style.display='none'">&times;</button>
        </div>
        <form class="modal-body" method="post" action="${pageContext.request.contextPath}/messages">
            <input type="hidden" name="csrfToken" value="${csrfToken}">

            <div class="form-group">
                <label>收件人</label>
                <input type="text" name="username" value="${composeTo}" placeholder="输入用户名..." required>
            </div>

            <div class="form-group">
                <label>主题（可选）</label>
                <input type="text" name="subject" placeholder="简短描述这条私信...">
            </div>

            <div class="form-group">
                <label>消息内容</label>
                <textarea name="content" rows="6" placeholder="写下你的私信..." required></textarea>
            </div>

            <div class="modal-footer">
                <button type="button" class="btn" onclick="document.getElementById('new-message-modal').style.display='none'">取消</button>
                <button type="submit" class="btn btn-primary">
                    <i data-lucide="send"></i> 发送
                </button>
            </div>
        </form>
    </div>
</div>

<!-- 会话操作菜单 -->
<div class="dropdown-menu" id="thread-menu" style="display: none;">
    <a href="#" data-action="pin"><i data-lucide="pin"></i> 置顶会话</a>
    <a href="#" data-action="archive"><i data-lucide="archive"></i> 归档会话</a>
    <a href="#" data-action="mute"><i data-lucide="bell-off"></i> 消息免打扰</a>
    <hr>
    <a href="#" data-action="delete" class="text-danger"><i data-lucide="trash-2"></i> 删除会话</a>
</div>

<script>
// 会话搜索功能
(function() {
    const searchInput = document.getElementById('chat-search-input');
    if (!searchInput) return;

    searchInput.addEventListener('input', function() {
        const query = this.value.toLowerCase().trim();
        const channels = document.querySelectorAll('.chat-channel');
        let visibleCount = 0;

        channels.forEach(function(channel) {
            const username = (channel.dataset.username || '').toLowerCase();
            const displayName = (channel.dataset.displayname || '').toLowerCase();
            const preview = (channel.dataset.preview || '').toLowerCase();

            const matches = username.includes(query) ||
                          displayName.includes(query) ||
                          preview.includes(query);

            if (matches) {
                channel.style.display = 'grid';
                visibleCount++;
            } else {
                channel.style.display = 'none';
            }
        });

        // 显示无结果提示
        let noResultsMsg = document.getElementById('no-search-results');
        if (query && visibleCount === 0) {
            if (!noResultsMsg) {
                noResultsMsg = document.createElement('div');
                noResultsMsg.id = 'no-search-results';
                noResultsMsg.className = 'empty-state';
                noResultsMsg.textContent = '没有找到匹配的会话';
                document.getElementById('chat-channels-list').appendChild(noResultsMsg);
            }
            noResultsMsg.style.display = 'block';
        } else if (noResultsMsg) {
            noResultsMsg.style.display = 'none';
        }
    });
})();

// 会话管理菜单
let currentThreadId = null;

function showThreadMenu(event, threadId) {
    event.preventDefault();
    event.stopPropagation();

    currentThreadId = threadId;
    const menu = document.getElementById('thread-menu');
    const rect = event.target.closest('button').getBoundingClientRect();

    menu.style.display = 'block';
    menu.style.left = rect.left + 'px';
    menu.style.top = (rect.bottom + 5) + 'px';
}

// 点击其他地方关闭菜单
document.addEventListener('click', function(e) {
    const menu = document.getElementById('thread-menu');
    if (menu && !menu.contains(e.target)) {
        menu.style.display = 'none';
    }
});

// 处理菜单操作
document.querySelectorAll('#thread-menu a').forEach(function(link) {
    link.addEventListener('click', function(e) {
        e.preventDefault();
        const action = this.dataset.action;
        if (currentThreadId && action) {
            handleThreadAction(currentThreadId, action);
        }
        document.getElementById('thread-menu').style.display = 'none';
    });
});

function handleThreadAction(threadId, action) {
    const contextPath = '${pageContext.request.contextPath}';

    fetch(contextPath + '/api/messages/threads/' + threadId + '/' + action, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(function(response) {
        return response.json();
    })
    .then(function(data) {
        if (data.success) {
            alert(data.message);
            // 刷新页面或更新UI
            if (action === 'delete' || action === 'archive') {
                window.location.href = contextPath + '/messages';
            } else {
                window.location.reload();
            }
        } else {
            alert('操作失败: ' + data.message);
        }
    })
    .catch(function(error) {
        alert('操作失败，请稍后重试');
    });
}

// 引用回复功能
function quoteMessage(messageId, senderName, messageBody) {
    document.getElementById('quotedMessageId').value = messageId;
    document.getElementById('quoted-sender-name').textContent = senderName;
    document.getElementById('quoted-content').textContent = messageBody;
    document.getElementById('quoted-preview').style.display = 'block';
    document.querySelector('.chat-form textarea').focus();
}

function clearQuote() {
    document.getElementById('quotedMessageId').value = '';
    document.getElementById('quoted-preview').style.display = 'none';
}
</script>

<%@ include file="/WEB-INF/views/layout/footer.jsp" %>
