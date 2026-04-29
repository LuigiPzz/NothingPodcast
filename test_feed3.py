import urllib.request

feed_url = "https://digitalia.fm/feeds/digitalia.xml"
feed_req = urllib.request.Request(feed_url, headers={'User-Agent': 'Mozilla/5.0'})
with urllib.request.urlopen(feed_req) as feed_resp:
    feed_content = feed_resp.read().decode()
    if 'chapter' in feed_content.lower():
        print("YES chapters exist!")
        import re
        matches = re.findall(r'<[^>]*chapter[^>]*>', feed_content, re.IGNORECASE)
        print("Tags:", matches[:10])
    else:
        print("NO chapters AT ALL in the entire feed.")
