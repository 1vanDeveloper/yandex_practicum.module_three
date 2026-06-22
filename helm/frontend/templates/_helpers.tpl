{{- define "frontend.env" -}}
- name: SPRING_PROFILES_ACTIVE
  value: "kubernetes"
- name: SPRING_APPLICATION_NAME
  value: "frontend"
- name: GATEWAY_SERVICE_NAME
  value: "gateway"
- name: SPRING_CLOUD_CONSUL_HOST
  value: "{{ .Values.global.consul.host }}"
- name: SPRING_CLOUD_CONSUL_PORT
  value: "{{ .Values.global.consul.port }}"
- name: SPRING_CLOUD_CONSUL_DISCOVERY_ENABLED
  value: "true"
- name: SPRING_CLOUD_CONSUL_DISCOVERY_PREFER_IP_ADDRESS
  value: "true"
- name: SPRING_CLOUD_LOADBALANCER_ENABLED
  value: "true"
- name: SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KEYCLOAK_ISSUER_URI
  value: "{{ .Values.global.keycloak.url }}/realms/{{ .Values.global.keycloak.realm }}"
- name: SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KEYCLOAK_AUTHORIZATION_URI
  value: "{{ .Values.global.keycloak.url }}/realms/{{ .Values.global.keycloak.realm }}/protocol/openid-connect/auth"
- name: SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KEYCLOAK_TOKEN_URI
  value: "{{ .Values.global.keycloak.url }}/realms/{{ .Values.global.keycloak.realm }}/protocol/openid-connect/token"
- name: SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KEYCLOAK_USER_INFO_URI
  value: "{{ .Values.global.keycloak.url }}/realms/{{ .Values.global.keycloak.realm }}/protocol/openid-connect/userinfo"
- name: SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KEYCLOAK_JWK_SET_URI
  value: "{{ .Values.global.keycloak.url }}/realms/{{ .Values.global.keycloak.realm }}/protocol/openid-connect/certs"
- name: SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KEYCLOAK_USER_NAME_ATTRIBUTE
  value: "preferred_username"
- name: SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI
  value: "{{ .Values.global.keycloak.url }}/realms/{{ .Values.global.keycloak.realm }}"
- name: SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI
  value: "{{ .Values.global.keycloak.url }}/realms/{{ .Values.global.keycloak.realm }}/protocol/openid-connect/certs"
{{- end -}}
