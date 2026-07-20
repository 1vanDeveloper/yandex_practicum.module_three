#!/usr/bin/env python3
import requests
import json

BASE_URL = "http://localhost:30561/api"
AUTH = ("admin", "admin")
HEADERS = {"kbn-xsrf": "true", "Content-Type": "application/json"}
INDEX_ID = "b56f3975-dd67-4c2d-a213-19ae5db97765"

def create_saved_search(title, description, query, columns):
    payload = {
        "attributes": {
            "title": title,
            "description": description,
            "hits": 0,
            "columns": columns,
            "sort": [["@timestamp", "desc"]],
            "version": 1,
            "kibanaSavedObjectMeta": {
                "searchSourceJSON": json.dumps({
                    "indexRefName": "kibanaSavedObjectMeta.searchSourceJSON.index",
                    "filter": [],
                    "query": {"query": query, "language": "kuery"}
                })
            }
        },
        "references": [{
            "name": "kibanaSavedObjectMeta.searchSourceJSON.index",
            "type": "index-pattern",
            "id": INDEX_ID
        }]
    }
    resp = requests.post(f"{BASE_URL}/saved_objects/search", headers=HEADERS, auth=AUTH, json=payload)
    data = resp.json()
    print(f"Created '{title}': {data.get('id', 'error')}")
    return data.get('id')

# Create saved searches
searches = [
    ("Logs: accounts", "Logs from accounts service", "service: accounts", ["level", "traceId", "spanId", "message"]),
    ("Logs: cash", "Logs from cash service", "service: cash", ["level", "traceId", "spanId", "message"]),
    ("Logs: transfer", "Logs from transfer service", "service: transfer", ["level", "traceId", "spanId", "message"]),
    ("Logs: notifications", "Logs from notifications service", "service: notifications", ["level", "traceId", "spanId", "message"]),
    ("Logs: gateway", "Logs from gateway service", "service: gateway", ["level", "traceId", "spanId", "message"]),
    ("Logs: frontend", "Logs from frontend service", "service: frontend", ["level", "traceId", "spanId", "message"]),
]

for title, desc, query, cols in searches:
    create_saved_search(title, desc, query, cols)

print("\nAll saved searches created!")
