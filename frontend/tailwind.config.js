/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/**/*.{js,jsx,ts,tsx}"],
  daisyui: {
    themes: [
      {
        mytheme: {
          "primary": "#b5cc18",
          "base-100": "#070707",
          "base-200": "#202020",
          "base-content": "#f5f5f5",
        },
      },
    ],
  },
  plugins: [require("daisyui")],
}

