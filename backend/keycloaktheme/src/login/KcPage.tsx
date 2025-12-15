import {lazy, Suspense} from "react";
import type {ClassKey} from "keycloakify/login";
import {getKcContext} from "keycloakify/login";
import type {KcContext} from "./KcContext";
import {useI18n} from "./i18n";
import DefaultPage from "keycloakify/login/DefaultPage";
import Template from "keycloakify/login/Template";

const Login = lazy(() => import("./pages/Login"));

const UserProfileFormFields = lazy(
    () => import("keycloakify/login/UserProfileFormFields")
);

const doMakeUserConfirmPassword = true;

export default function KcPage() {
    const {kcContext} = getKcContext<KcContext>({
        mockPageId: "login.ftl",
    });

    const {i18n} = useI18n({kcContext});

    if (kcContext === undefined) {
        return <div>Keycloak context is undefined</div>;
    }

    return (
        <Suspense fallback={<div className="kc-login-spinner">Loading...</div>}>
            {(() => {
                switch (kcContext.pageId) {
                    case "login.ftl":
                        return (
                            <Login
                                kcContext={kcContext}
                                i18n={i18n}
                                classes={classes}
                                Template={Template}
                                doUseDefaultCss={true}
                            />
                        );
                    default:
                        return (
                            <DefaultPage
                                kcContext={kcContext}
                                i18n={i18n}
                                classes={classes}
                                Template={Template}
                                doUseDefaultCss={true}
                                UserProfileFormFields={UserProfileFormFields}
                                doMakeUserConfirmPassword={doMakeUserConfirmPassword}
                            />
                        );
                }
            })()}
        </Suspense>
    );
}

const classes = {
    kcHtmlClass: "",
    kcBodyClass: "",
    kcFormClass: "",
    kcFormHeaderClass: "",
    kcFormGroupClass: "",
    kcLabelClass: "",
    kcInputClass: "",
    kcButtonClass: "",
    kcButtonPrimaryClass: "",
    kcButtonBlockClass: "",
    kcButtonLargeClass: "",
} satisfies { [key in ClassKey]?: string };