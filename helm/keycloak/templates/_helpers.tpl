{{- define "keycloak.env" -}}
- name: KEYCLOAK_ADMIN
  value: "{{ .Values.keycloak.adminUser }}"
- name: KEYCLOAK_ADMIN_PASSWORD
  value: "{{ .Values.keycloak.adminPassword }}"
- name: KC_DB
  value: "{{ .Values.keycloak.db }}"
- name: KC_HOSTNAME_URL
  value: "{{ .Values.keycloak.hostnameUrl }}"
- name: KC_HOSTNAME_ADMIN_URL
  value: "{{ .Values.keycloak.hostnameAdminUrl }}"
- name: KC_HOSTNAME_STRICT
  value: "{{ .Values.keycloak.hostnameStrict }}"
- name: KC_HOSTNAME_STRICT_HTTPS
  value: "{{ .Values.keycloak.hostnameStrictHttps }}"
{{- end -}}
