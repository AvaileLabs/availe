import type {ExtendKcContext} from "keycloakify/login";
import {KcEnvName, ThemeName} from "./kc.gen.tsx";

export type KcContextExtension = {
    themeName: ThemeName;
    properties: Record<KcEnvName, string> & {};
};

export type KcContextExtensionPerPage = {};

export type KcContext = ExtendKcContext<KcContextExtension, KcContextExtensionPerPage>;