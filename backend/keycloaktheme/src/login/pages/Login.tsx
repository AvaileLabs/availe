import {type FormEventHandler, useState} from "react";
import {clsx} from "keycloakify/tools/clsx";
import {useConstCallback} from "keycloakify/tools/useConstCallback";
import type {PageProps} from "keycloakify/login/pages/PageProps";
import type {KcContext} from "../KcContext";
import type {I18n} from "../i18n";

export default function Login(props: PageProps<Extract<KcContext, { pageId: "login.ftl" }>, I18n>) {
    const {kcContext, i18n, doUseDefaultCss, Template, classes} = props;

    const {social, realm, url, usernameHidden, login, auth, registrationDisabled} = kcContext;

    const {msg, msgStr} = i18n;

    const [isLoginButtonDisabled, setIsLoginButtonDisabled] = useState(false);

    const onSubmit = useConstCallback<FormEventHandler<HTMLFormElement>>(event => {
        event.preventDefault();
        setIsLoginButtonDisabled(true);
        const formElement = event.target as HTMLFormElement;
        formElement.submit();
    });

    return (
        <Template
            kcContext={kcContext}
            i18n={i18n}
            doUseDefaultCss={doUseDefaultCss}
            classes={classes}
            displayInfo={social?.displayInfo}
            headerNode={msg("loginTitle")}
        >
            <div id="kc-form">

                {/* MOVED REGISTRATION BUTTON TO TOP FOR BETTER UX */}
                {realm.password && realm.registrationAllowed && !registrationDisabled && (
                    <div className={clsx(classes?.kcFormGroupClass)} style={{marginBottom: "2rem"}}>
                        <div id="kc-registration">
                            <a
                                tabIndex={1}
                                href={url.registrationUrl}
                                className={clsx(
                                    classes?.kcButtonClass,
                                    classes?.kcButtonPrimaryClass,
                                    classes?.kcButtonBlockClass,
                                    classes?.kcButtonLargeClass
                                )}
                                style={{
                                    backgroundColor: "#4CAF50",
                                    borderColor: "#4CAF50",
                                    textAlign: "center",
                                    display: "block",
                                    textDecoration: "none",
                                    fontWeight: "bold"
                                }}
                            >
                                {msg("doRegister")}
                            </a>
                        </div>
                        <div style={{textAlign: "center", marginTop: "10px", color: "#666"}}>
                            <span>Or log in with your account</span>
                        </div>
                    </div>
                )}

                <div id="kc-form-wrapper">
                    {realm.password && (
                        <form
                            id="kc-form-login"
                            onSubmit={onSubmit}
                            action={url.loginAction}
                            method="post"
                        >
                            <div className={clsx(classes?.kcFormGroupClass)}>
                                <label htmlFor="username" className={clsx(classes?.kcLabelClass)}>
                                    {!realm.loginWithEmailAllowed
                                        ? msg("username")
                                        : !realm.registrationEmailAsUsername
                                            ? msg("usernameOrEmail")
                                            : msg("email")}
                                </label>
                                <input
                                    tabIndex={2}
                                    id="username"
                                    className={clsx(classes?.kcInputClass)}
                                    name="username"
                                    defaultValue={login.username ?? ""}
                                    type="text"
                                    autoFocus
                                    autoComplete="username"
                                />
                            </div>

                            <div className={clsx(classes?.kcFormGroupClass)}>
                                <label htmlFor="password" className={clsx(classes?.kcLabelClass)}>
                                    {msg("password")}
                                </label>
                                <input
                                    tabIndex={3}
                                    id="password"
                                    className={clsx(classes?.kcInputClass)}
                                    name="password"
                                    type="password"
                                    autoComplete="current-password"
                                />
                            </div>

                            <div className={clsx(classes?.kcFormGroupClass, "kcFormSettingClass")}>
                                <div id="kc-form-options">
                                    {realm.rememberMe && !usernameHidden && (
                                        <div className="checkbox">
                                            <label>
                                                <input
                                                    tabIndex={4}
                                                    id="rememberMe"
                                                    name="rememberMe"
                                                    type="checkbox"
                                                    defaultChecked={!!login.rememberMe}
                                                />{" "}
                                                {msg("rememberMe")}
                                            </label>
                                        </div>
                                    )}
                                </div>
                                <div className="kcFormOptionsWrapperClass">
                                    {realm.resetPasswordAllowed && (
                                        <span>
                                            <a tabIndex={5} href={url.loginResetCredentialsUrl}>
                                                {msg("doForgotPassword")}
                                            </a>
                                        </span>
                                    )}
                                </div>
                            </div>

                            <div id="kc-form-buttons" className={clsx(classes?.kcFormGroupClass)}>
                                <input
                                    type="hidden"
                                    id="id-hidden-input"
                                    name="credentialId"
                                    {...(auth.selectedCredential !== undefined
                                        ? {
                                            value: auth.selectedCredential
                                        }
                                        : {})}
                                />
                                <input
                                    tabIndex={6}
                                    disabled={isLoginButtonDisabled}
                                    className={clsx(
                                        classes?.kcButtonClass,
                                        classes?.kcButtonPrimaryClass,
                                        classes?.kcButtonBlockClass,
                                        classes?.kcButtonLargeClass
                                    )}
                                    name="login"
                                    id="kc-login"
                                    type="submit"
                                    value={msgStr("doLogIn")}
                                />
                            </div>
                        </form>
                    )}
                </div>

                {/* SOCIAL PROVIDERS */}
                {realm.password && social?.providers !== undefined && (
                    <div
                        id="kc-social-providers"
                        className={clsx("kcFormSocialAccountSectionClass", "para")}
                    >
                        <hr/>
                        <h4>{msg("identity-provider-login-label")}</h4>
                        <ul className={clsx("kcFormSocialAccountListClass", social.providers.length > 3 && "list-grid")}>
                            {social.providers.map((...[p]) => (
                                <li key={p.alias}>
                                    <a
                                        id={`social-${p.alias}`}
                                        className={clsx(
                                            "kcFormSocialAccountListButtonClass",
                                            social.providers && social.providers.length > 3 && "grid"
                                        )}
                                        type="button"
                                        href={p.loginUrl}
                                    >
                                        {p.iconClasses && <i className={clsx("kcCommonLogoIdP", p.iconClasses)}
                                                             aria-hidden="true"></i>}
                                        <span
                                            className={clsx("kcFormSocialAccountNameClass", p.iconClasses && "kc-social-icon-text")}
                                            dangerouslySetInnerHTML={{__html: p.displayName}}
                                        ></span>
                                    </a>
                                </li>
                            ))}
                        </ul>
                    </div>
                )}
            </div>
        </Template>
    );
}