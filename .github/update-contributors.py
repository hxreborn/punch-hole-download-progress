#!/usr/bin/env python3
import io
import json
import os
import re
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "contributors"
AVATARS = OUT / "avatars"
URL_MAP = OUT / ".avatar-urls.json"
SNAPSHOT = OUT / "contributors.json"
AVATAR_PX = 96
ATTEMPTS = 5
LOGIN_RE = re.compile(r"^[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?$")


def repo_slug():
    remote = subprocess.run(
        ["git", "-C", str(ROOT), "remote", "get-url", "origin"],
        capture_output=True,
        text=True,
        check=True,
    ).stdout.strip()
    slug = remote.removesuffix(".git").replace("git@github.com:", "").replace("https://github.com/", "")
    if slug.count("/") != 1:
        sys.exit(f"cannot read owner/repo from origin: {remote}")
    return slug


def request(url):
    headers = {"Accept": "application/vnd.github+json", "User-Agent": "update-contributors"}
    token = os.environ.get("GITHUB_TOKEN")
    if token:
        headers["Authorization"] = f"Bearer {token}"
    return urllib.request.urlopen(urllib.request.Request(url, headers=headers), timeout=30)


def fetch_stats(slug):
    url = f"https://api.github.com/repos/{slug}/stats/contributors"
    for attempt in range(ATTEMPTS):
        with request(url) as response:
            if response.status == 200:
                return json.load(response)
        if attempt < ATTEMPTS - 1:
            time.sleep(2**attempt)
    sys.exit(f"{url} still answered 202 after {ATTEMPTS} attempts")


def is_bot(author):
    login = author["login"]
    return author.get("type") == "Bot" or login.endswith("-bot") or login.endswith("[bot]")


def summarize(stats, slug):
    owner = slug.split("/")[0].lower()
    people = []
    for entry in stats:
        author = entry.get("author")
        if not author or is_bot(author):
            continue
        if not LOGIN_RE.match(author["login"]):
            sys.exit(f"unexpected login {author['login']!r}")
        people.append(
            {
                "login": author["login"],
                "owner": author["login"].lower() == owner,
                "added": sum(week["a"] for week in entry["weeks"]),
                "removed": sum(week["d"] for week in entry["weeks"]),
                "url": author["html_url"],
                "avatar": f"avatars/{author['login']}.webp",
                "_avatar_url": author["avatar_url"],
            }
        )
    people.sort(key=lambda person: (-person["added"], person["login"]))
    return people


def download_avatar(avatar_url, target):
    with request(f"{avatar_url}&s={AVATAR_PX}") as response:
        image = Image.open(io.BytesIO(response.read()))
    image.convert("RGBA").resize((AVATAR_PX, AVATAR_PX)).save(target, "WEBP", lossless=True)


def main():
    slug = repo_slug()
    people = summarize(fetch_stats(slug), slug)
    if not people:
        sys.exit("refusing to write an empty contributor list")

    AVATARS.mkdir(parents=True, exist_ok=True)
    known = json.loads(URL_MAP.read_text()) if URL_MAP.exists() else {}
    urls = {}
    for person in people:
        login = person["login"]
        target = AVATARS / f"{login}.webp"
        avatar_url = person.pop("_avatar_url")
        if known.get(login) != avatar_url or not target.exists():
            download_avatar(avatar_url, target)
        urls[login] = avatar_url

    keep = {f"{person['login']}.webp" for person in people}
    for stale in AVATARS.glob("*.webp"):
        if stale.name not in keep:
            stale.unlink()

    URL_MAP.write_text(json.dumps(urls, indent=2, sort_keys=True) + "\n")
    SNAPSHOT.write_text(json.dumps(people, indent=2) + "\n")


if __name__ == "__main__":
    try:
        main()
    except urllib.error.URLError as error:
        sys.exit(f"GitHub request failed: {error}")
