{{- define "postgresql.env" -}}
- name: POSTGRES_USER
  value: "{{ .Values.postgresql.username }}"
- name: POSTGRES_PASSWORD
  value: "{{ .Values.postgresql.password }}"
- name: POSTGRES_DB
  value: "{{ .Values.postgresql.database }}"
- name: PGDATA
  value: "{{ .Values.postgresql.dataDir }}"
{{- end -}}
