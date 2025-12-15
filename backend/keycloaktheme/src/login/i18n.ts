import {i18nBuilder} from "keycloakify/login";
import {ThemeName} from "./kc.gen.tsx";

const {useI18n, ofTypeI18n} = i18nBuilder.withThemeName<ThemeName>().build();

type I18n = typeof ofTypeI18n;

export {useI18n, type I18n};