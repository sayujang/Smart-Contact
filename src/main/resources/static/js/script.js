let currentTheme = getTheme();
applyTheme(currentTheme);
setupThemeToggle();

function setupThemeToggle() {
  const changeThemeButton = document.querySelector("#theme_change");
  if (!changeThemeButton) return; // guard if button not present

  // set initial button text to reflect the *action* (what clicking will do)
  const span = changeThemeButton.querySelector('span');
  if (span) span.textContent = currentTheme === 'light' ? 'Dark' : 'Light';

  changeThemeButton.addEventListener("click", () => {
    const oldTheme = currentTheme;
    currentTheme = currentTheme === "dark" ? "light" : "dark";
    setTheme(currentTheme);
    document.documentElement.classList.replace(oldTheme, currentTheme); // document.documentElement =<html> element//here classlist is the css styling parameters in class
    if (span) span.textContent = currentTheme === 'light' ? 'Dark' : 'Light';
  });
}

function applyTheme(theme) {
  // remove both just in case, then add the chosen one
  document.documentElement.classList.remove('light', 'dark');
  document.documentElement.classList.add(theme);
}

function setTheme(theme) {
  try {
    localStorage.setItem("theme", theme);//localstorage(withing the browser) stores only key-value pair in strings
  } catch (e) {
    // localStorage might be blocked — fail silently or handle it
    console.warn("Could not save theme:", e);
  }
}

function getTheme() {
  try {
    const theme = localStorage.getItem("theme");
    return theme || "light";
  } catch (e) {
    return "light";
  }
}
