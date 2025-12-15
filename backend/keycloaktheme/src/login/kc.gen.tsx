import {lazy, type ReactNode, Suspense} from "react";
import {KcContext} from "./KcContext.ts";

export type ThemeName = "availe-theme";

export const themeNames: ThemeName[] = ["availe-theme"];

export type KcEnvName = never;

export const kcEnvNames: KcEnvName[] = [];

export const kcEnvDefaults: Record<KcEnvName, string> = {};


declare global {
    interface Window {
        kcContext?: KcContext;
    }
}

export const KcLoginPage = lazy(() => import("./login/KcLoginPage.tsx"));

export function KcPage(props: { kcContext: KcContext; fallback?: ReactNode }) {
    const {kcContext, fallback} = props;
    return (
        <Suspense fallback={fallback}>
            {(() => {
                switch (kcContext.themeType) {
                    case "login":
                        return <KcLoginPage kcContext={kcContext}/>;
                }
            })()}
        </Suspense>
    );
}

export const BASE_URL = import.meta.env.BASE_URL;

export type {Meta, StoryObj} from "@storybook/react-vite";