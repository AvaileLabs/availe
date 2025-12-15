import {createRoot} from "react-dom/client";
import {StrictMode} from "react";
import KcPage from "./login/KcPage";

const rootElement = document.getElementById("root");

if (rootElement) {
    createRoot(rootElement).render(
        <StrictMode>
            <KcPage/>
        </StrictMode>
    );
}