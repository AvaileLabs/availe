import {createUseI18n} from "keycloakify/login";

export const {useI18n, ofTypeI18n} = createUseI18n({
    en: {
        loginTitle: "Log in to Availe",
        registerTitle: "New here? Join Availe",
        doRegister: "Create New Account"
    }
});

export type I18n = typeof ofTypeI18n;