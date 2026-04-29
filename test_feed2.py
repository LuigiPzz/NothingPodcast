import urllib.request
import json
import re

feed_url = "https://digitalia.fm/feeds/digitalia.xml"
feed_req = urllib.request.Request(feed_url, headers={'User-Agent': 'Mozilla/5.0'})
with urllib.request.urlopen(feed_req) as feed_resp:
    feed_content = feed_resp.read().decode()
    
    # Extract the first item's description
    item_match = re.search(r'<item>(.*?)</item>', feed_content, re.DOTALL)
    if item_match:
        item = item_match.group(1)
        desc_match = re.search(r'<description><!\[CDATA\[(.*?)\]\]></description>', item, re.DOTALL)
        if desc_match:
            print("DESCRIPTION:", desc_match.group(1)[:500])
            
        # extract timestamps from description!
        timestamps = re.findall(r'\d{1,2}:\d{2}', item)
        print("TIMESTAMPS:", timestamps)
