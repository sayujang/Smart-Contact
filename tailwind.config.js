module.exports = {
  darkMode: 'class',
  content: [
    "./src/main/resources/templates/**/*.html",
    "./src/main/resources/static/**/*.js"
  ],
  theme: {
    extend: {},
  },
  plugins: [
    require('flowbite/plugin')
  ],
}