const form = document.querySelector("#create-form");
const originalUrlInput = document.querySelector("#original-url");
const expiresAtInput = document.querySelector("#expires-at");
const urlList = document.querySelector("#url-list");
const clickList = document.querySelector("#click-list");
const selectedCode = document.querySelector("#selected-code");
const refreshButton = document.querySelector("#refresh-button");
const rowTemplate = document.querySelector("#url-row-template");
const createdResult = document.querySelector("#created-result");
const createdLink = document.querySelector("#created-link");
const copyCreatedLink = document.querySelector("#copy-created-link");

async function api(path, options = {}) {
    const response = await fetch(path, {
        headers: { "Content-Type": "application/json" },
        ...options
    });

    if (!response.ok) {
        const error = await response.json().catch(() => ({ message: "Request failed" }));
        throw new Error(error.message || "Request failed");
    }

    return response.json();
}

async function loadDashboard() {
    const [stats, urls] = await Promise.all([
        api("/api/dashboard"),
        api("/api/urls?size=50")
    ]);

    document.querySelector("#total-urls").textContent = stats.totalUrls;
    document.querySelector("#total-clicks").textContent = stats.totalClicks;
    document.querySelector("#clicks-today").textContent = stats.clicksToday;

    renderUrls(urls.content || []);
}

function renderUrls(urls) {
    urlList.innerHTML = "";

    if (urls.length === 0) {
        urlList.innerHTML = '<p class="muted">No short URLs yet.</p>';
        return;
    }

    urls.forEach((url) => {
        const row = rowTemplate.content.firstElementChild.cloneNode(true);
        row.dataset.code = url.shortCode;
        row.dataset.shortUrl = url.shortUrl;
        row.querySelector(".short-url").textContent = url.shortUrl;
        row.querySelector(".original-url").textContent = url.originalUrl;
        row.querySelector(".url-meta").innerHTML = `${url.clickCount} clicks<br>${expiryText(url)}`;
        row.querySelector(".url-details").addEventListener("click", async () => {
            await selectUrl(url.shortCode, row);
        });
        row.querySelector(".copy-link").addEventListener("click", async () => {
            await copyShortUrl(url.shortUrl, row);
        });
        urlList.appendChild(row);
    });
}

async function selectUrl(shortCode, row) {
    document.querySelectorAll(".url-row").forEach((item) => item.classList.remove("active"));
    row.classList.add("active");
    selectedCode.textContent = shortCode;
    clickList.textContent = "Loading...";

    const analytics = await api(`/api/urls/${shortCode}/analytics`);
    const recentClicks = analytics.recentClicks || [];
    if (recentClicks.length === 0) {
        clickList.innerHTML = '<p class="muted">No clicks recorded for this URL.</p>';
        return;
    }

    clickList.innerHTML = recentClicks.map((click) => `
        <article class="click-event">
            <strong>${formatDate(click.clickedAt)}</strong>
            <span>${escapeHtml(click.ipAddress || "Unknown IP")}</span>
            <span>${escapeHtml(click.referrer || "Direct visit")}</span>
        </article>
    `).join("");
}

function expiryText(url) {
    if (!url.expiresAt) {
        return "No expiry";
    }
    const text = `Expires ${formatDate(url.expiresAt)}`;
    return url.expired ? `<span class="expired">Expired</span>` : text;
}

function formatDate(value) {
    return new Intl.DateTimeFormat(undefined, {
        dateStyle: "medium",
        timeStyle: "short"
    }).format(new Date(value));
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

form.addEventListener("submit", async (event) => {
    event.preventDefault();

    const payload = {
        originalUrl: originalUrlInput.value,
        expiresAt: expiresAtInput.value ? expiresAtInput.value : null
    };

    try {
        const created = await api("/api/urls", {
            method: "POST",
            body: JSON.stringify(payload)
        });
        form.reset();
        showCreatedLink(created.shortUrl);
        await loadDashboard();
    } catch (error) {
        alert(error.message);
    }
});

refreshButton.addEventListener("click", loadDashboard);
copyCreatedLink.addEventListener("click", async () => {
    await copyText(createdLink.textContent);
    copyCreatedLink.textContent = "Copied";
    setTimeout(() => {
        copyCreatedLink.textContent = "Copy";
    }, 1600);
});
loadDashboard().catch((error) => {
    urlList.innerHTML = `<p class="muted">${escapeHtml(error.message)}</p>`;
});

function showCreatedLink(shortUrl) {
    createdLink.textContent = shortUrl;
    createdResult.hidden = false;
}

async function copyShortUrl(shortUrl, row) {
    await copyText(shortUrl);
    const status = row.querySelector(".copy-status");
    status.textContent = "Copied";
    setTimeout(() => {
        status.textContent = "";
    }, 1600);
}

async function copyText(value) {
    if (navigator.clipboard && window.isSecureContext) {
        await navigator.clipboard.writeText(value);
        return;
    }

    const input = document.createElement("input");
    input.value = value;
    input.style.position = "fixed";
    input.style.opacity = "0";
    document.body.appendChild(input);
    input.select();
    document.execCommand("copy");
    input.remove();
}
