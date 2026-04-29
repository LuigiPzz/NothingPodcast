import urllib.request
import json
import re

search_url = "https://itunes.apple.com/search?term=digitalia&media=podcast&limit=1"
req = urllib.request.Request(search_url, headers={'User-Agent': 'Mozilla/5.0'})
with urllib.request.urlopen(req) as response:
    search_result = json.loads(response.read().decode())
    
if search_result['results']:
    feed_url = search_result['results'][0]['feedUrl']
    print("Feed URL:", feed_url)
    
    feed_req = urllib.request.Request(feed_url, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(feed_req) as feed_resp:
        feed_content = feed_resp.read().decode()
        
        matches = re.findall(r'<button[^>]*>', feed_content)
        print("Found", len(matches), "buttons")
        if matches:
            print("First button:", matches[0])
            
        chap_matches = re.findall(r'data-start-time', feed_content)
        print("Found", len(chap_matches), "data-start-time attributes")
else:
    print("No results")
