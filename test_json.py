import urllib.request

req = urllib.request.Request("https://cdn.dgt.fm/digitalia821.json", headers={'User-Agent': 'Mozilla/5.0'})
with urllib.request.urlopen(req) as resp:
    print(resp.read().decode())
