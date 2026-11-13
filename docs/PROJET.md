# Chkoun Ykhelles ? (appli Android de NexPhase Group)

## Pourquoi
Le compte Google Play Console « NexPhase Group » (organisation NG CORP) risque d'être clôturé pour inactivité : il faut publier une appli **avant le 28/12/2026**. On en profite pour faire une appli grand public qui fait connaître l'agence.

## L'appli
Qui paie l'addition entre amis ? Le sort décide (roue, duel de réflexes, téléphone secoué), ou on partage au dirham près. Le verdict se partage en story (image 1080 × 1920 signée NexPhase Group). Un bilan montre combien on a payé et combien on a épargné.

## Fonctionnalités (v1.0.0)
- Accueil : fond de pièces qui se retournent sous le doigt et onde dorée toutes les 5 s, titre à poids variable, entrée en cascade.
- Qui est à table : mon prénom (obligatoire, pour le bilan) puis jusqu'à 12 personnes, choix du mode.
- Roue : tirage équitable décidé à l'avance, lancer au geste ou au bouton, ampoules en chenillard, vibration à chaque case, rotation douce au repos.
- Duel : deux finalistes tirés au sort, verre de thé rouge puis vert, « trop tôt » = perdu.
- Secouer : accéléromètre (sans autorisation) ou bouton, défilement des prénoms qui ralentit.
- Verdict : ticket qui tombe, pluie de pièces, montant facultatif, partage de l'image, enregistrement automatique dans l'historique.
- Partager l'addition : montant, personnes, pourboire 0/5/10/15 %, enregistrement de ma part.
- Mon bilan : payé, épargné, barre, historique (modifier le montant, supprimer, tout effacer).

## Technique
- Kotlin, vues Android natives dessinées en code, **aucune bibliothèque externe**. APK final : ~510 Ko.
- minSdk 26, targetSdk/compileSdk 36 (obligatoire pour les nouvelles applis depuis le 31/08/2026).
- AGP 8.13.0, Kotlin 2.2.20, Gradle 8.14.3. Polices Rubik (variable) et Lalezar (licence OFL, `docs/OFL-*.txt`).
- Données : SharedPreferences sur le téléphone, exclues des sauvegardes cloud.
- Aucune autorisation dans le manifeste. Partage via un ContentProvider non exporté qui ne sert qu'un fichier (`verdict.png`).
- Mode démo (version de test uniquement, `src/debug`) : `adb shell am start -n ma.nexphasegroup.chkoun/.MainActivity --es demo bilan`.

## Commandes
- Compiler la version de test : `gradlew assembleDebug` (JAVA_HOME = JBR d'Android Studio).
- Version Play Store : `gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`.
- Signature : clé d'importation dans `%USERPROFILE%\.android-keys\chkoun\` (hors projet, jamais dans Git). **À sauvegarder sur une clé USB.**
- Visuels du store : `store/src/rendre.py` avec le Python de Scrapling (Playwright).

## Décisions
Voir `docs/DECISIONS.md`.

## État d'avancement
- 13/11/2026 : maquette v1 validée (sauf l'étoile à 8 branches, retirée : connotation religieuse). App codée, testée sur émulateur (Android 14), version finale signée. Visuels du store prêts.
- À faire : mettre en ligne la politique de confidentialité, créer la fiche dans la Play Console, envoyer l'AAB.
- 13/11/2026 : code publié sur GitHub, dépôt public https://github.com/dragnosii/chkoun-ykhelles
