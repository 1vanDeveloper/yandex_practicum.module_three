{{- define "gateway.env" -}}
- name: SPRING_PROFILES_ACTIVE
  value: "kubernetes"
- name: SPRING_APPLICATION_NAME
  value: "gateway"
- name: JWT_SECRET
  value: "mySecretKeyForJWTTokenGenerationMustBeLongEnough"
- name: JWT_EXPIRATION
  value: "86400000"
- name: SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI
  value: "{{ .Values.global.keycloak.url }}/realms/{{ .Values.global.keycloak.realm }}"
- name: SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI
  value: "{{ .Values.global.keycloak.url }}/realms/{{ .Values.global.keycloak.realm }}/protocol/openid-connect/certs"
- name: SPRING_CLOUD_CONSUL_HOST
  value: "{{ .Values.global.consul.host }}"
- name: SPRING_CLOUD_CONSUL_PORT
  value: "{{ .Values.global.consul.port }}"
- name: SPRING_CLOUD_CONSUL_DISCOVERY_ENABLED
  value: "true"
- name: SPRING_CLOUD_CONSUL_DISCOVERY_PREFER_IP_ADDRESS
  value: "true"
- name: SPRING_CLOUD_GATEWAY_DISCOVERY_LOCATOR_ENABLED
  value: "true"
- name: SPRING_CLOUD_GATEWAY_DISCOVERY_LOCATOR_LOWER_CASE_SERVICE_ID
  value: "true"
- name: MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE
  value: "health,info,routes"
- name: MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS
  value: "always"
{{- end -}}
