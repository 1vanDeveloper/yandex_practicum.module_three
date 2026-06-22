{{- define "accounts.env" -}}
- name: SPRING_PROFILES_ACTIVE
  value: "kubernetes"
- name: SPRING_DATASOURCE_URL
  value: "jdbc:postgresql://{{ .Values.global.postgresql.host }}:{{ .Values.global.postgresql.port }}/{{ .Values.global.postgresql.database }}?currentSchema=accounts"
- name: SPRING_DATASOURCE_USERNAME
  value: "{{ .Values.global.postgresql.username }}"
- name: SPRING_DATASOURCE_PASSWORD
  value: "{{ .Values.global.postgresql.password }}"
- name: SPRING_JPA_HIBERNATE_DDL_MODE
  value: "validate"
- name: SPRING_CLOUD_CONSUL_HOST
  value: "{{ .Values.global.consul.host }}"
- name: SPRING_CLOUD_CONSUL_PORT
  value: "{{ .Values.global.consul.port }}"
- name: SPRING_CLOUD_CONSUL_DISCOVERY_PREFER_IP_ADDRESS
  value: "true"
- name: SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI
  value: "{{ .Values.global.keycloak.url }}/realms/{{ .Values.global.keycloak.realm }}"
- name: SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI
  value: "{{ .Values.global.keycloak.url }}/realms/{{ .Values.global.keycloak.realm }}/protocol/openid-connect/certs"
{{- end -}}
