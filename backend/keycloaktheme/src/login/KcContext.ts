import type {ExtendKcContext} from "keycloakify/login";
import type {KcEnvName, ThemeName} from "keycloakify/dist/bin/keycloakify/generateFtl";

export type KcContextExtension = {
    themeName?: ThemeName;
    properties: {
        [key in KcEnvName]: string;
    };
};

export type KcContext = ExtendKcContext<KcContextExtension, {}>;