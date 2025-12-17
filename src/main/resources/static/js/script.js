document.addEventListener("DOMContentLoaded", () => {
  // -------------------------
  // DOM Helpers
  // -------------------------
  const $ = (sel) => document.querySelector(sel);
  const $$ = (sel) => Array.from(document.querySelectorAll(sel));

  const contentArea = $("#contentArea");
  const detailApplyBtn = $("#detailApplyBtn");

  // Home tabs
  const homeTabs = {
    team: $("#teamBox"),
    board: $("#boardBox"),
  };

  // Recruit create modal
  const recruitModal = $("#recruitModal");
  const recruitForm = $("#recruitForm");
  const closeRecruitBtn = $("#closeRecruitBtn");

  // Detail modal
  const detailModal = $("#detailModal");
  const detailCloseBtn = $("#detailCloseBtn");
  const detailCompleteBtn = $("#detailCompleteBtn");

  const detailTitle = $("#detailTitle");
  const detailCreator = $("#detailCreator");
  const detailSpots = $("#detailSpots");
  const detailStatus = $("#detailStatus");
  const detailCountdown = $("#detailCountdown");
  const detailDesc = $("#detailDesc");

  const isHome = !!(homeTabs.team && homeTabs.board);
  const isMyPage = !!($("#teamTab") && $("#friendsTab"));

  // -------------------------
  // State
  // -------------------------
  const state = {
    home: { page: 0, size: 10 },
    my: { page: 0, size: 5 },
    applied: { page: 0, size: 5 },
    countdownTimer: null,
    selectedFriendIds: new Set(),
    selectedFriendNames: new Map(),
    view: "myPosts", // "homeTeam" | "myPosts" | "myApplied" | friends...
  };

  // -------------------------
  // Modal Position (현재 위치 기준 가운데)
  // -------------------------
  function centerModalAtViewport(modalEl) {
    if (!modalEl) return;
    const modalContent = modalEl.querySelector(".modal-content");
    if (!modalContent) return;

    modalEl.style.position = "fixed";
    modalEl.style.inset = "0";
    modalEl.style.display = "flex";
    modalEl.style.alignItems = "center";
    modalEl.style.justifyContent = "center";

    modalContent.style.maxHeight = "80vh";
    modalContent.style.overflow = "auto";
  }

  if (recruitModal) centerModalAtViewport(recruitModal);
  if (detailModal) centerModalAtViewport(detailModal);

  function openCenteredModal(modalEl) {
    if (!modalEl) return;
    const modalContent = modalEl.querySelector(".modal-content");
    if (!modalContent) return;

    modalEl.classList.remove("hidden");

    // scroll 기준 중앙(absolute)로 열고 싶다면 유지
    const scrollY = window.scrollY || document.documentElement.scrollTop;
    const viewportHeight = window.innerHeight;

    modalContent.style.position = "absolute";
    modalContent.style.top = `${scrollY + viewportHeight / 2}px`;
    modalContent.style.transform = "translateY(-50%)";
  }

  // -------------------------
  // CSRF Helpers
  // -------------------------
  function getCsrf() {
    const token = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
    const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");
    if (!token || !header) return null;
    return { token, header };
  }

  function withCsrfHeaders(opts = {}) {
    const csrf = getCsrf();
    if (!csrf) return opts;

    const headers = new Headers(opts.headers || {});
    headers.set(csrf.header, csrf.token);
    return { ...opts, headers };
  }

  // -------------------------
  // UI Utils
  // -------------------------
  function setLoading() {
    if (!contentArea) return;
    contentArea.innerHTML = "<p>불러오는 중...</p>";
  }

  function setError(msg = "데이터를 불러오는데 실패했습니다.") {
    if (!contentArea) return;
    contentArea.innerHTML = `<p>${msg}</p>`;
  }

  function setActiveTab(tabA, tabB, activeEl) {
    if (!tabA || !tabB || !activeEl) return;
    [tabA, tabB].forEach((el) => {
      el.classList.add("tab-btn");
      el.classList.toggle("active-tab", el === activeEl);
    });
  }

  function setActive3(a, b, c, active) {
    [a, b, c].forEach((el) => {
      if (!el) return;
      el.classList.add("tab-btn");
      el.classList.toggle("active-tab", el === active);
    });
  }

  // -------------------------
  // API
  // -------------------------
  async function fetchJson(url, opts = {}) {
    const merged = withCsrfHeaders({
      credentials: "same-origin",
      ...opts,
    });

    const res = await fetch(url, merged);
    const ct = (res.headers.get("content-type") || "").toLowerCase();
    const rawText = await res.text();

    const looksLikeHtml =
      ct.includes("text/html") ||
      rawText.trim().startsWith("<!doctype") ||
      rawText.trim().startsWith("<html");

    if (looksLikeHtml) {
      console.error("❗️서버가 JSON 대신 HTML을 반환했습니다.", {
        url,
        status: res.status,
        contentType: ct,
        preview: rawText.slice(0, 200),
      });
      throw new Error("HTML response (not JSON). Check login/authority/http-https.");
    }

    let data = null;
    if (ct.includes("application/json")) data = JSON.parse(rawText);
    else {
      try {
        data = JSON.parse(rawText);
      } catch {
        data = rawText;
      }
    }

    if (!res.ok) {
      console.error("API ERROR:", { url, status: res.status, body: data });
      throw new Error(`HTTP ${res.status}`);
    }
    return data;
  }

  // -------------------------
  // Pagination Render
  // -------------------------
  function renderPagination(pageObj) {
    const current = pageObj.number ?? 0;
    const total = pageObj.totalPages ?? 1;
    const isFirst = !!pageObj.first;
    const isLast = !!pageObj.last;

    return `
      <div class="pagination">
        <span class="page-info">${current + 1} / ${total}</span>
        <button id="prevPageBtn" class="page-btn" ${isFirst ? "disabled" : ""}>◀</button>
        <button id="nextPageBtn" class="page-btn" ${isLast ? "disabled" : ""}>▶</button>
      </div>
    `;
  }

  // -------------------------
  // Countdown
  // -------------------------
  function startCountdown() {
    if (state.countdownTimer) {
      clearInterval(state.countdownTimer);
      state.countdownTimer = null;
    }

    const elements = $$(".countdown");
    if (elements.length === 0) return;

    const pad = (n) => String(n).padStart(2, "0");

    const update = () => {
      const now = new Date();

      elements.forEach((el) => {
		const card = el.closest(".recruit-card") || el.closest(".modal-content");
        const status = card?.getAttribute("data-status");

        if (status === "CLOSED" || status === "COMPLETED") {
          el.textContent = "마감";
          return;
        }

        const endStr = el.getAttribute("data-end-time");
        if (!endStr) {
          el.textContent = "-";
          return;
        }

        const end = new Date(endStr);
        const diff = end - now;

        if (diff <= 0) {
          el.textContent = "마감";
          const c = el.closest(".card");
          if (c) {
            c.classList.remove("open");
            c.classList.add("closed");
            c.setAttribute("data-status", "COMPLETED");
          }
          return;
        }

        const t = Math.floor(diff / 1000);
        const d = Math.floor(t / 86400);
        const h = Math.floor((t % 86400) / 3600);
        const m = Math.floor((t % 3600) / 60);
        const s = t % 60;

        el.textContent = `${d > 0 ? d + "일 " : ""}${pad(h)}시간 ${pad(m)}분 ${pad(s)}초`;
      });
    };

    update();
    state.countdownTimer = setInterval(update, 1000);
  }

  // -------------------------
  // Helpers
  // -------------------------
  function escapeHtml(str) {
    return String(str ?? "")
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#039;");
  }

  function hasApplied(applications) {
    if (!Array.isArray(applications)) return false;
    if (CURRENT_USER_ID == null) return false;
    return applications.some((app) => Number(app.userId) === Number(CURRENT_USER_ID));
  }

  function currentUserNameSafe() {
    if (typeof CURRENT_USER_NAME === "undefined") return null;
    return CURRENT_USER_NAME == null ? null : String(CURRENT_USER_NAME);
  }

  // -------------------------
  // Recruit API
  // -------------------------
  async function applyRecruit(recruitId) {
    return await fetchJson(`/recruitments/apply/${recruitId}`, { method: "POST" });
  }

  async function completeRecruit(recruitId) {
    await fetchJson(`/recruitments/${recruitId}/complete`, { method: "POST" });
  }

  // -------------------------
  // Friend Picker (모집하기 모달 내부)
  // -------------------------
  recruitModal?.addEventListener("click", (e) => {
    const openBtn = e.target.closest("#openFriendPickerBtn");
    if (openBtn) {
      loadFriendPicker();
      return;
    }

    const pickBtn = e.target.closest(".btn-pick-friend");
    if (pickBtn) {
      const uid = Number(pickBtn.dataset.userId);
      const nm = pickBtn.dataset.name || "";
      if (!Number.isFinite(uid)) return;

      state.selectedFriendIds.add(uid);
      state.selectedFriendNames.set(uid, nm);

      pickBtn.disabled = true;
      pickBtn.textContent = "선택됨";

      renderSelectedFriends();
      return;
    }

    const removeBtn = e.target.closest(".btn-remove-picked");
    if (removeBtn) {
      const uid = Number(removeBtn.dataset.userId);
      state.selectedFriendIds.delete(uid);
      state.selectedFriendNames.delete(uid);
      renderSelectedFriends();
      return;
    }
  });

  async function loadFriendPicker() {
    const picker = $("#friendPicker");
    const listWrap = $("#friendPickerList");
    if (!picker || !listWrap) return;

    picker.classList.remove("hidden");
    listWrap.innerHTML = `<p>불러오는 중...</p>`;

    try {
      const res = await fetchJson(`/friends/list`);
      const list = res?.data ?? [];

      if (!Array.isArray(list) || list.length === 0) {
        listWrap.innerHTML = `<p class="muted">친구가 없습니다.</p>`;
        return;
      }

      listWrap.innerHTML = `
        <div class="card-container">
          ${list
            .map((f) => {
              const name =
                Number(f.senderId) === Number(CURRENT_USER_ID)
                  ? escapeHtml(f.receiverName)
                  : escapeHtml(f.senderName);

              const friendUserId =
                Number(f.senderId) === Number(CURRENT_USER_ID) ? f.receiverId : f.senderId;

              const disabled = state.selectedFriendIds.has(Number(friendUserId));
              return `
                <div class="card friend-card" style="cursor:default;">
                  <div class="ctitle">${name}</div>
                  <div class="card-actions">
                    <button class="btn-pick-friend"
                      data-user-id="${friendUserId}"
                      data-name="${name}"
                      ${disabled ? "disabled" : ""}>
                      ${disabled ? "선택됨" : "넣기"}
                    </button>
                  </div>
                </div>
              `;
            })
            .join("")}
        </div>
      `;
    } catch (e) {
      console.error(e);
      listWrap.innerHTML = `<p class="muted">친구 목록을 불러오지 못했습니다.</p>`;
    }
  }

  function renderSelectedFriends() {
    const selected = $("#selectedFriends");
    if (!selected) return;

    const ids = Array.from(state.selectedFriendIds);
    if (ids.length === 0) {
      selected.innerHTML = `<p class="muted">선택된 친구 없음</p>`;
      return;
    }

    selected.innerHTML = `
      <div style="display:flex; gap:6px; flex-wrap:wrap;">
        ${ids
          .map((id) => {
            const name = state.selectedFriendNames.get(Number(id)) || `ID:${id}`;
            return `
              <span style="border:1px solid #ddd; padding:4px 8px; border-radius:999px;">
                ${escapeHtml(name)}
                <button type="button" class="btn-remove-picked" data-user-id="${id}" style="margin-left:6px;">x</button>
              </span>
            `;
          })
          .join("")}
      </div>
    `;
  }

  function resetFriendSelectionUI() {
    state.selectedFriendIds.clear();
    state.selectedFriendNames.clear();

    const selected = $("#selectedFriends");
    if (selected) selected.innerHTML = `<p class="muted">선택된 친구 없음</p>`;

    const picker = $("#friendPicker");
    if (picker) picker.classList.add("hidden");
  }

  // -------------------------
  // Friends (마이페이지)
  // -------------------------
  function renderFriendsLayout() {
    contentArea.innerHTML = `
      <div class="list-header">
        <h2>친구</h2>
      </div>
      <div id="friendsView"></div>
    `;
  }

  async function loadFriendFind() {
    state.view = "friendsFind";
    renderFriendsLayout();

    const view = $("#friendsView");
    if (!view) return;

    if (CURRENT_USER_ID === null) {
      view.innerHTML = `<p>로그인 후 이용 가능합니다.</p>`;
      return;
    }

    view.innerHTML = `
      <h3>친구 찾기</h3>
      <div style="display:flex; gap:8px; width:100%; align-items:center;">
        <input id="friendNameInput" type="text" placeholder="이름 입력"
          style="flex:1; padding:6px 8px; border-radius:6px; border:1px solid #ccc;">
        <button id="sendFriendReqBtn" class="page-btn">전송</button>
      </div>

      <hr style="width:100%; margin:16px 0;">

      <h3>친구 추천</h3>
      <div id="recommendList"><p>불러오는 중...</p></div>
    `;

    $("#sendFriendReqBtn")?.addEventListener("click", async () => {
      const name = $("#friendNameInput")?.value?.trim();
      if (!name) return alert("이름을 입력해주세요.");

      const me = currentUserNameSafe();
      if (me && String(name).trim() === me.trim()) {
        return alert("자기 자신에게는 친구 요청을 보낼 수 없습니다.");
      }

      try {
        await fetchJson(`/friends/request?receiverName=${encodeURIComponent(name)}`, {
          method: "POST",
        });

        alert("친구 요청을 전송했습니다.");
        $("#friendNameInput").value = "";
      } catch (e) {
        console.error(e);
        alert("친구 요청 전송에 실패했습니다.");
      }
    });

    try {
      const res = await fetchJson(`/friends/recommend?limit=10`);
      const list = res?.data ?? [];

      const wrap = $("#recommendList");
      if (!wrap) return;

      if (!Array.isArray(list) || list.length === 0) {
        wrap.innerHTML = `<p class="muted">추천 친구가 없습니다.</p>`;
        return;
      }

      wrap.innerHTML = `
        <div class="card-container">
          ${list
            .map((r) => {
              const nm = r.userName;
              const common = r.commonCount;
              return `
                <div class="card friend-card" style="cursor:default;">
                  <div class="ctitle">${escapeHtml(nm)}</div>
                  <div class="meta">공통 친구 ${common}명</div>
                  <div class="card-actions">
                    <button class="btn-send-recommend" data-name="${escapeHtml(nm)}">요청</button>
                  </div>
                </div>
              `;
            })
            .join("")}
        </div>
      `;

      wrap.addEventListener("click", async (e) => {
        const btn = e.target.closest(".btn-send-recommend");
        if (!btn) return;

        const receiverName = btn.dataset.name;
        if (!receiverName) return;

        const me = currentUserNameSafe();
        if (me && String(receiverName).trim() === me.trim()) {
          return alert("자기 자신에게는 친구 요청을 보낼 수 없습니다.");
        }

        try {
          await fetchJson(`/friends/request?receiverName=${encodeURIComponent(receiverName)}`, {
            method: "POST",
          });
          alert(`${receiverName}님에게 친구 요청을 보냈습니다.`);
          btn.disabled = true;
          btn.textContent = "전송됨";
        } catch (err) {
          console.error(err);
          alert("친구 요청 전송에 실패했습니다.");
        }
      });
    } catch (e) {
      console.error(e);
      $("#recommendList").innerHTML = `<p class="muted">추천 목록을 불러오지 못했습니다.</p>`;
    }
  }

  async function loadFriendRequests() {
    state.view = "friendsRequests";
    renderFriendsLayout();

    const view = $("#friendsView");
    if (!view) return;

    if (CURRENT_USER_ID === null) {
      view.innerHTML = `<p>로그인 후 이용 가능합니다.</p>`;
      return;
    }

    view.innerHTML = `
      <h3>받은 친구 요청</h3>
      <div id="requestList"><p>불러오는 중...</p></div>
    `;

    try {
      const res = await fetchJson(`/friends/requests`);
      const list = res?.data ?? [];

      const wrap = $("#requestList");
      if (!wrap) return;

      if (!Array.isArray(list) || list.length === 0) {
        wrap.innerHTML = `<p class="muted">받은 요청이 없습니다.</p>`;
        return;
      }

      wrap.innerHTML = `
        <div class="card-container">
          ${list
            .map((f) => {
              const friendshipId = f.id;
              const sender = f.senderName;
              return `
                <div class="card friend-card" style="cursor:default;">
                  <div class="ctitle">${escapeHtml(sender)}</div>
                  <div class="card-actions">
                    <button class="btn-accept" data-id="${friendshipId}">수락</button>
                    <button class="btn-reject" data-id="${friendshipId}">거절</button>
                  </div>
                </div>
              `;
            })
            .join("")}
        </div>
      `;

      wrap.addEventListener("click", async (e) => {
        const acceptBtn = e.target.closest(".btn-accept");
        const rejectBtn = e.target.closest(".btn-reject");
        if (!acceptBtn && !rejectBtn) return;

        const id = (acceptBtn || rejectBtn).dataset.id;
        if (!id) return alert("friendshipId가 없습니다.");

        try {
          if (acceptBtn) {
            await fetchJson(`/friends/${id}/accept`, { method: "POST" });
            alert("친구 요청을 수락했습니다.");
          } else {
            await fetchJson(`/friends/${id}/reject`, { method: "POST" });
            alert("친구 요청을 거절했습니다.");
          }
          loadFriendRequests();
        } catch (err) {
          console.error(err);
          alert("처리에 실패했습니다.");
        }
      });
    } catch (e) {
      console.error(e);
      $("#requestList").innerHTML = `<p class="muted">요청 목록을 불러오지 못했습니다.</p>`;
    }
  }

  async function loadFriendList() {
    state.view = "friendsList";
    renderFriendsLayout();

    const view = $("#friendsView");
    if (!view) return;

    if (CURRENT_USER_ID === null) {
      view.innerHTML = `<p>로그인 후 이용 가능합니다.</p>`;
      return;
    }

    view.innerHTML = `
      <h3>내 친구 목록</h3>
      <div id="friendList"><p>불러오는 중...</p></div>
    `;

    try {
      const res = await fetchJson(`/friends/list`);
      const list = res?.data ?? [];

      const wrap = $("#friendList");
      if (!wrap) return;

      if (!Array.isArray(list) || list.length === 0) {
        wrap.innerHTML = `<p class="muted">친구가 없습니다.</p>`;
        return;
      }

      wrap.innerHTML = `
        <div class="card-container">
          ${list
            .map((f) => {
              const friendshipId = f.id;
              const name =
                Number(f.senderId) === Number(CURRENT_USER_ID)
                  ? escapeHtml(f.receiverName)
                  : escapeHtml(f.senderName);

              return `
                <div class="card friend-card" style="cursor:default;">
                  <div class="ctitle">${name}</div>
                  <div class="card-actions">
                    <button class="btn-delete-friend" data-id="${friendshipId}">삭제</button>
                  </div>
                </div>
              `;
            })
            .join("")}
        </div>
      `;

      wrap.addEventListener("click", async (e) => {
        const btn = e.target.closest(".btn-delete-friend");
        if (!btn) return;

        const id = btn.dataset.id;
        if (!id) return alert("friendshipId가 없습니다.");

        if (!confirm("친구를 삭제하시겠습니까?")) return;

        try {
          await fetchJson(`/friends/${id}`, { method: "DELETE" });
          alert("친구가 삭제되었습니다.");
          loadFriendList();
        } catch (err) {
          console.error(err);
          alert("친구 삭제에 실패했습니다.");
        }
      });
    } catch (e) {
      console.error(e);
      $("#friendList").innerHTML = `<p class="muted">친구 목록을 불러오지 못했습니다.</p>`;
    }
  }

  // -------------------------
  // Render Recruit Cards (✅ recruit-card로 스코프 분리)
  // -------------------------
  function renderRecruitCards(list, opts = { mode: "HOME" }) {
    const mode = opts.mode ?? "HOME";
    list.sort((a, b) => Number(b.id) - Number(a.id));

    return list
      .map((item) => {
        const isClosed = item.status === "CLOSED" || item.status === "COMPLETED";

        const meName = currentUserNameSafe();
        const isOwner =
          (CURRENT_USER_ID != null &&
            item.creatorId != null &&
            Number(item.creatorId) === Number(CURRENT_USER_ID)) ||
          (meName && item.creatorName && String(item.creatorName) === meName);

        let ownerBtns = "";

        if ((mode === "HOME" || mode === "MYPAGE_MYPOSTS") && isOwner && !isClosed) {
          ownerBtns = `
            <div class="card-actions">
              <button class="btn-complete" data-id="${item.id}">마감</button>
            </div>
          `;
        }

        const onOff = isClosed ? "closed" : "open";

        return `
          <div class="card recruit-card ${onOff}" data-status="${item.status}" data-id="${item.id}">
            <div class="ctitle">${escapeHtml(item.title ?? "")}</div>
            <div class="authorSpotsTime">
              <div class="meta">작성자: ${escapeHtml(item.creatorName ?? "")}</div>
              <div class="meta">인원: ${item.currentSpots} / ${item.totalSpots}</div>
              <div class="meta">
                마감까지:
                <span class="countdown" data-end-time="${item.endTime ?? ""}"></span>
              </div>
            </div>
            ${ownerBtns}
          </div>
        `;
      })
      .join("");
  }

  // -------------------------
  // DETAIL MODAL
  // -------------------------
  async function openDetailModal(recruitId) {
    const idNum = Number(recruitId);
    if (!Number.isFinite(idNum)) return;

    const body = await fetchJson(`/recruitments/${idNum}`);
    const d = body?.data ?? body;
    if (!d) throw new Error("No detail data");

    let applications = Array.isArray(d.apllications) ? d.apllications : [];

    if (applications.length === 0 && d.creatorName) {
      applications = [{ name: d.creatorName, userId: d.creatorId ?? null, order: 1 }];
    }

    const isClosed = d.status === "CLOSED" || d.status === "COMPLETED";
    const meName = currentUserNameSafe();
    const isOwner = meName && d.creatorName && String(d.creatorName) === meName;

    const isFull = Number(d.currentSpots ?? 0) >= Number(d.totalSpots ?? 0);

    const isMemberByName =
      !!meName && applications.some((a) => String(a?.name ?? "").trim() === meName.trim());

    const applied = hasApplied(applications);

    if (detailTitle) detailTitle.textContent = d.title ?? "-";
    if (detailCreator) detailCreator.textContent = d.creatorName ?? "-";
    if (detailSpots) detailSpots.textContent = `${d.currentSpots ?? 0} / ${d.totalSpots ?? 0}`;
    if (detailStatus) detailStatus.textContent = d.status ?? "-";
    if (detailDesc) detailDesc.textContent = d.description ?? "-";

    if (detailCountdown) detailCountdown.setAttribute("data-end-time", d.endTime ?? "");
    if (detailModal) detailModal.setAttribute("data-status", d.status ?? "");

    const appsWrap = $("#detailApps");
    if (appsWrap) {
      appsWrap.innerHTML = applications.length
        ? `<ul class="apps-list">
            ${applications
              .map((a) => {
                const nm = escapeHtml(a?.name ?? "알 수 없음");
                const ord = a?.order != null ? ` (#${a.order})` : "";
                return `<li>${nm}${ord}</li>`;
              })
              .join("")}
          </ul>`
        : `<p class="muted">팀원이 없습니다.</p>`;
    }

    if (detailApplyBtn) {
      detailApplyBtn.style.display = "none";
      detailApplyBtn.disabled = false;
      detailApplyBtn.textContent = "신청하기";
      detailApplyBtn.dataset.id = String(d.id ?? idNum);
      detailApplyBtn.classList.add("btn-apply");
    }
    if (detailCompleteBtn) {
      detailCompleteBtn.style.display = "none";
      detailCompleteBtn.dataset.id = String(d.id ?? idNum);
    }

    if (state.view === "myPosts") {
      if (detailCompleteBtn) detailCompleteBtn.style.display = !isClosed ? "inline-block" : "none";
    } else if (state.view === "myApplied") {
      // no buttons
    } else {
      if (isOwner) {
        if (detailCompleteBtn) detailCompleteBtn.style.display = !isClosed ? "inline-block" : "none";
      } else {
        if (detailApplyBtn) {
          if (isClosed || isMemberByName) {
            detailApplyBtn.style.display = "none";
          } else {
            detailApplyBtn.style.display = "inline-block";

            if (applied || isFull) {
              detailApplyBtn.disabled = true;
              detailApplyBtn.textContent = applied ? "신청완료" : "정원마감";
            } else {
              detailApplyBtn.disabled = false;
              detailApplyBtn.textContent = "신청하기";
            }
          }
        }
      }
    }

    openCenteredModal(detailModal);
    startCountdown();
  }

  function closeDetailModal() {
    detailModal?.classList.add("hidden");
    if (detailCountdown) detailCountdown.setAttribute("data-end-time", "");
    if (detailCompleteBtn) detailCompleteBtn.dataset.id = "";
  }

  detailCloseBtn?.addEventListener("click", closeDetailModal);
  detailModal?.addEventListener("click", (e) => {
    if (e.target === detailModal) closeDetailModal();
  });

  detailApplyBtn?.addEventListener("click", async () => {
    if (detailApplyBtn.disabled) return;
    const id = detailApplyBtn.dataset.id;
    if (!id) return;

    if (!confirm("이 모집에 신청하시겠습니까?")) return;

    try {
      await applyRecruit(id);
      alert("신청이 완료되었습니다!");
      detailApplyBtn.textContent = "신청완료";
      detailApplyBtn.disabled = true;
      refreshCurrentList();
    } catch (e) {
      console.error(e);
      alert("신청에 실패했습니다.");
    }
  });

  detailCompleteBtn?.addEventListener("click", async () => {
    const id = detailCompleteBtn.dataset.id;
    if (!id) return;

    if (!confirm("이 모집을 마감하시겠습니까?")) return;
    try {
      await completeRecruit(id);
      closeDetailModal();
      refreshCurrentList();
    } catch (e) {
      console.error(e);
      alert("마감 처리 중 오류가 발생했습니다.");
    }
  });

  // -------------------------
  // HOME
  // -------------------------
  async function loadHomeTeam(page = state.home.page) {
    state.view = "homeTeam";
    setLoading();

    try {
      const body = await fetchJson(`/recruitments/lists?page=${page}&size=${state.home.size}`);

      if (!body?.success || !body.data || !Array.isArray(body.data.content)) {
        setError("응답 형식이 올바르지 않습니다.");
        return;
      }

      state.home.page = body.data.number ?? page;
      const list = body.data.content;

      contentArea.innerHTML = `
        <div class="list-header">
          <h2>밥친구 모집글</h2>
          <button id="openRecruitFormBtn" class="recruit-btn">모집하기</button>
        </div>

        ${
          list.length === 0
            ? `<p class="muted" style="margin-top:12px;">현재 모집 중인 글이 없습니다.</p>`
            : `
              <div class="card-container">
                ${renderRecruitCards(list, { mode: "HOME" })}
              </div>
              ${renderPagination(body.data)}
            `
        }
      `;

      $("#openRecruitFormBtn")?.addEventListener("click", () => {
        openCenteredModal(recruitModal);
        resetFriendSelectionUI();
      });

      if (list.length !== 0) {
        $("#prevPageBtn")?.addEventListener("click", () => {
          if (!body.data.first) loadHomeTeam(state.home.page - 1);
        });
        $("#nextPageBtn")?.addEventListener("click", () => {
          if (!body.data.last) loadHomeTeam(state.home.page + 1);
        });
      }

      startCountdown();
    } catch (e) {
      console.error(e);
      setError();
    }
  }

  function showHomeBoard() {
    state.view = "homeTeam";
    contentArea.innerHTML = `
      <h2>info</h2>
      <p>누구랑 밥먹을지 고민중인 당신!</p>
      <p>빠르게 밥메이트를 찾아봅시다!</p>
    `;
  }

  function initHome() {
    homeTabs.team.classList.add("tab-btn");
    homeTabs.board.classList.add("tab-btn");

    homeTabs.team.addEventListener("click", () => {
      setActiveTab(homeTabs.team, homeTabs.board, homeTabs.team);
      loadHomeTeam(0);
    });

    homeTabs.board.addEventListener("click", () => {
      setActiveTab(homeTabs.team, homeTabs.board, homeTabs.board);
      showHomeBoard();
    });

    setActiveTab(homeTabs.team, homeTabs.board, homeTabs.team);
    loadHomeTeam(0);

    closeRecruitBtn?.addEventListener("click", () => {
      recruitModal?.classList.add("hidden");
      recruitForm?.reset();
      resetFriendSelectionUI();
    });

    recruitModal?.addEventListener("click", (e) => {
      if (e.target === recruitModal) {
        recruitModal.classList.add("hidden");
        recruitForm?.reset();
        resetFriendSelectionUI();
      }
    });

    recruitForm?.addEventListener("submit", async (e) => {
      e.preventDefault();

      if (CURRENT_USER_ID === null) {
        alert("로그인 후 이용해주세요.");
        return;
      }

      const formData = new FormData(recruitForm);
      const title = (formData.get("title") || "").toString().trim();
      const description = (formData.get("description") || "").toString().trim();
      const totalSpots = Number(formData.get("totalSpots"));

      const endHours = Number(formData.get("endHours") || 0);
      const endMinutes = Number(formData.get("endMinutes") || 0);

      if (!title) return alert("제목을 입력해주세요.");
      if (!description) return alert("내용을 입력해주세요.");
      if (!Number.isFinite(totalSpots) || totalSpots < 2)
        return alert("모집 인원을 확인해주세요. (최소 2명)");

      const totalMins = endHours * 60 + endMinutes;
      if (!Number.isFinite(totalMins) || totalMins <= 0) {
        return alert("마감 시간을 0분보다 크게 입력해주세요.");
      }

      const end = new Date(Date.now() + totalMins * 60 * 1000);
      const pad = (n) => String(n).padStart(2, "0");
      const endTime =
        `${end.getFullYear()}-${pad(end.getMonth() + 1)}-${pad(end.getDate())}` +
        `T${pad(end.getHours())}:${pad(end.getMinutes())}:${pad(end.getSeconds())}`;

      const preApplierid = Array.from(state.selectedFriendIds);
      const payload = { title, description, totalSpots, endTime, preApplierid };

      try {
        await fetchJson(`/recruitments/posts`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload),
        });

        recruitModal?.classList.add("hidden");
        recruitForm?.reset();
        resetFriendSelectionUI();
        loadHomeTeam(0);
      } catch (err) {
        console.error(err);
        alert("모집글 작성 중 오류가 발생했습니다.");
      }
    });
  }

  // -------------------------
  // MYPAGE
  // -------------------------
  async function loadMyPosts(page = state.my.page) {
    state.view = "myPosts";
    if (CURRENT_USER_ID === null) {
      contentArea.innerHTML = `<h2>내가 쓴 글</h2><p>로그인 후 이용 가능합니다.</p>`;
      return;
    }

    setLoading();
    const body = await fetchJson(`/recruitments/my/posts?page=${page}&size=${state.my.size}`);

    if (!body?.success || !body.data || !Array.isArray(body.data.content)) {
      setError("응답 형식이 올바르지 않습니다.");
      return;
    }

    state.my.page = body.data.number ?? page;

    const list = body.data.content;
    if (list.length === 0) {
      contentArea.innerHTML = `<h2>내가 쓴 글</h2><p>아직 작성한 글이 없습니다.</p>`;
      return;
    }

    contentArea.innerHTML = `
      <h2>내가 쓴 글</h2>
      <div class="card-container">
        ${renderRecruitCards(list, { mode: "MYPAGE_MYPOSTS" })}
      </div>
      ${renderPagination(body.data)}
    `;

    $("#prevPageBtn")?.addEventListener("click", () => {
      if (!body.data.first) loadMyPosts(state.my.page - 1);
    });
    $("#nextPageBtn")?.addEventListener("click", () => {
      if (!body.data.last) loadMyPosts(state.my.page + 1);
    });

    startCountdown();
  }

  async function loadMyApplied(page = state.applied.page) {
    state.view = "myApplied";
    if (CURRENT_USER_ID === null) {
      contentArea.innerHTML = `<h2>내가 신청한 글</h2><p>로그인 후 이용 가능합니다.</p>`;
      return;
    }

    setLoading();
    const body = await fetchJson(
      `/recruitments/apply/my/ApplyList?page=${page}&size=${state.applied.size}`
    );

    if (!body?.success || !body.data || !Array.isArray(body.data.content)) {
      setError("응답 형식이 올바르지 않습니다.");
      return;
    }

    state.applied.page = body.data.number ?? page;

    const list = body.data.content;
    if (list.length === 0) {
      contentArea.innerHTML = `<h2>내가 신청한 글</h2><p>아직 신청한 글이 없습니다.</p>`;
      return;
    }

    contentArea.innerHTML = `
      <h2>내가 신청한 글</h2>
      <div class="card-container">
        ${renderRecruitCards(list, { mode: "MYPAGE_APPLIED" })}
      </div>
      ${renderPagination(body.data)}
    `;

    $("#prevPageBtn")?.addEventListener("click", () => {
      if (!body.data.first) loadMyApplied(state.applied.page - 1);
    });
    $("#nextPageBtn")?.addEventListener("click", () => {
      if (!body.data.last) loadMyApplied(state.applied.page + 1);
    });

    startCountdown();
  }

  function initMyPage() {
    const teamTab = $("#teamTab");
    const friendsTab = $("#friendsTab");

    const subWrap = $("#teamSubTabs");
    const myPostsTab = $("#myPostsTab");
    const myAppliedTab = $("#myAppliedTab");

    const friendsSubTabs = $("#friendsSubTabs");
    const friendFindTab = $("#friendFindTab");
    const friendRequestsTab = $("#friendRequestsTab");
    const friendListTab = $("#friendListTab");

    teamTab?.addEventListener("click", () => {
      setActiveTab(teamTab, friendsTab, teamTab);
      if (subWrap) subWrap.style.display = "flex";
      if (friendsSubTabs) friendsSubTabs.style.display = "none";

      setActiveTab(myPostsTab, myAppliedTab, myPostsTab);
      loadMyPosts(0);
    });

    friendsTab?.addEventListener("click", () => {
      setActiveTab(teamTab, friendsTab, friendsTab);
      if (subWrap) subWrap.style.display = "none";
      if (friendsSubTabs) friendsSubTabs.style.display = "flex";

      [friendFindTab, friendRequestsTab, friendListTab].forEach((el) =>
        el?.classList.remove("active-tab")
      );
      friendFindTab?.classList.add("active-tab");
      loadFriendFind();
    });

    friendFindTab?.addEventListener("click", () => {
      setActive3(friendFindTab, friendRequestsTab, friendListTab, friendFindTab);
      loadFriendFind();
    });

    friendRequestsTab?.addEventListener("click", () => {
      setActive3(friendFindTab, friendRequestsTab, friendListTab, friendRequestsTab);
      loadFriendRequests();
    });

    friendListTab?.addEventListener("click", () => {
      setActive3(friendFindTab, friendRequestsTab, friendListTab, friendListTab);
      loadFriendList();
    });

    myPostsTab?.addEventListener("click", () => {
      setActiveTab(myPostsTab, myAppliedTab, myPostsTab);
      loadMyPosts(0);
    });

    myAppliedTab?.addEventListener("click", () => {
      setActiveTab(myPostsTab, myAppliedTab, myAppliedTab);
      loadMyApplied(0);
    });

    setActiveTab(teamTab, friendsTab, teamTab);
    if (subWrap) subWrap.style.display = "flex";
    setActiveTab(myPostsTab, myAppliedTab, myPostsTab);
    loadMyPosts(0);
  }

  // -------------------------
  // Refresh current list
  // -------------------------
  function refreshCurrentList() {
    if (state.view === "homeTeam") loadHomeTeam(state.home.page);
    else if (state.view === "myPosts") loadMyPosts(state.my.page);
    else if (state.view === "myApplied") loadMyApplied(state.applied.page);
    else if (state.view === "friendsFind") loadFriendFind();
    else if (state.view === "friendsRequests") loadFriendRequests();
    else if (state.view === "friendsList") loadFriendList();
  }

  // -------------------------
  // GLOBAL EVENT DELEGATION
  // -------------------------
  contentArea?.addEventListener("click", async (e) => {
    const btn = e.target.closest(".btn-apply");
    if (!btn) return;

    e.stopPropagation();
    const id = btn.dataset.id;
    if (!id) return;

    if (!confirm("이 모집에 신청하시겠습니까?")) return;

    try {
      await applyRecruit(id);
      refreshCurrentList();
      alert("신청이 완료되었습니다!");
    } catch (err) {
      console.error(err);
      alert("신청에 실패했습니다. (정원 초과/중복 신청/로그인 필요 여부 확인)");
    }
  });

  contentArea?.addEventListener("click", (e) => {
    const target = e.target;
    if (target.classList.contains("btn-complete")) return;

	const card = el.closest(".recruit-card");
    if (card?.dataset?.id) {
      openDetailModal(card.dataset.id).catch((err) => {
        console.error(err);
        alert("상세 정보를 불러오지 못했습니다.");
      });
    }
  });

  contentArea?.addEventListener("click", (e) => {
    // 마감 버튼 클릭이면 카드 클릭으로 처리하지 않기
    if (e.target.closest(".btn-complete")) return;
    if (e.target.closest(".btn-apply")) return; // (선택) 신청 버튼도 카드 클릭 막기

    const card = e.target.closest(".recruit-card"); // ✅ 핵심 수정
    if (!card?.dataset?.id) return;

    openDetailModal(card.dataset.id).catch((err) => {
      console.error(err);
      alert("상세 정보를 불러오지 못했습니다.");
    });
  });

  console.log("CURRENT_USER_ID =", CURRENT_USER_ID);
  if (typeof CURRENT_USER_NAME !== "undefined") {
    console.log("CURRENT_USER_NAME =", CURRENT_USER_NAME);
  }

  // -------------------------
  // Init
  // -------------------------
  if (isHome) initHome();
  if (isMyPage) initMyPage();
});
