import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
    plugins: [react()],
    resolve: {
        alias: {
            // Optional alias example:
            // '@': new URL('./src', import.meta.url).pathname
        }
    },
    server: {
        port: 3000
    }
});
