"""Rend les visuels du Play Store en PNG à partir de visuels.html (navigateur local, aucun envoi).

Lancer : python -I rendre.py   (avec le Python de Scrapling qui contient Playwright)
"""
import http.server
import pathlib
import threading
from functools import partial

from playwright.sync_api import sync_playwright

ICI = pathlib.Path(__file__).resolve().parent
SORTIE = ICI.parent


def main():
    gestion = partial(http.server.SimpleHTTPRequestHandler, directory=str(ICI))
    serveur = http.server.ThreadingHTTPServer(("127.0.0.1", 0), gestion)
    threading.Thread(target=serveur.serve_forever, daemon=True).start()
    url = f"http://127.0.0.1:{serveur.server_address[1]}/visuels.html"
    with sync_playwright() as p:
        nav = p.chromium.launch()
        page = nav.new_page(viewport={"width": 1100, "height": 2000})
        page.goto(url)
        page.evaluate("document.fonts.ready")
        page.wait_for_timeout(500)
        for i in range(1, 7):
            page.locator(f"#s{i}").screenshot(path=str(SORTIE / f"capture-{i}.png"))
        page.locator("#icone").screenshot(path=str(SORTIE / "icone-512.png"))
        page.locator("#banniere").screenshot(path=str(SORTIE / "banniere-1024x500.png"))
        nav.close()
    serveur.shutdown()
    print("Visuels rendus dans", SORTIE)


if __name__ == "__main__":
    main()
