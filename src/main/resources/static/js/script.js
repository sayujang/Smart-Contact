let currentTheme=getTheme();
changeTheme();
function changeTheme()
{
    document.querySelector("html").classList.add(currentTheme);
    const changeThemeButton=document.querySelector("#theme_change");
    changeThemeButton.addEventListener("click",()=>{
        const oldTheme=currentTheme;
        if (currentTheme==="dark")
        {
            currentTheme="light";
        }
        else{
            currentTheme="dark";
        }
        setTheme(currentTheme);
        document.querySelector("html").classList.remove(oldTheme);
        document.querySelector("html").classList.add(currentTheme);
        changeThemeButton.querySelector('span').textContent=currentTheme=='light'?'Dark':'Light';
    })
} 
    function setTheme(theme)
    {
        localStorage.setItem("theme",theme);
    }
    function getTheme()
    {
        let theme=localStorage.getItem("theme");
        if(theme) return theme;
        else return "light";
    }
