// setLoginRedirect.js
document.addEventListener("DOMContentLoaded", () => {
    function setLoginRedirect() {
        const loginBtn = document.getElementById("loginBtn");
        if(loginBtn) {
            const redirectParam = encodeURIComponent(window.location.pathname);
            loginBtn.setAttribute("href", "/member/signIn?redirect=" + redirectParam);
        } else {
            setTimeout(setLoginRedirect, 50);
        }
    }
    setLoginRedirect();
});
