# Fiche Google Play : Chkoun Ykhelles

Tout ce qu'il faut copier dans la Play Console (compte « NexPhase Group », organisation NG CORP).

## Identité
- **Nom de l'appli** (30 caractères max) : `Chkoun Ykhelles : qui paie ?` (28)
- **Identifiant** : `ma.nexphasegroup.chkoun` (définitif)
- **Catégorie** : Divertissement
- **Coordonnées** : contact@nexphasegroup.ma, https://nexphasegroup.ma/
- **Politique de confidentialité** : https://nexphasegroup.ma/chkoun/confidentialite/ (branche `site` du dépôt, déployée par Git Hostinger dans `public_html/chkoun`, Node.js désactivé pour ce dossier)

## Description courte (80 caractères max)
Roue, duel ou partage : qui paie l'addition entre amis, sans dispute.

## Description longue (4000 caractères max)
L'addition arrive, personne ne bouge ? Chkoun Ykhelles tranche pour vous.

Ajoute les prénoms de ceux qui sont à table, puis choisis comment le sort décide :
- La roue : lance-la d'un geste. Le tirage est équitable et l'aiguille claque à chaque case.
- Le duel : deux finalistes, un verre de thé qui passe au vert. Le plus lent régale.
- Secouer : secoue le téléphone, il désigne celui qui paie.

Le verdict s'affiche sur un ticket que tu peux partager en story ou dans le groupe.

Tu préfères partager ? Saisis le montant, le nombre de personnes et le pourboire : chacun connaît sa part, au dirham près.

Ton bilan : l'appli garde l'historique de tes soirées et te montre combien tu as payé et combien tu as épargné grâce au sort.

Respect de ta vie privée :
- aucun compte, aucune publicité, aucun achat ;
- aucune autorisation demandée ;
- fonctionne sans internet ;
- tes données restent sur ton téléphone.

Une appli NexPhase Group, faite à Casablanca.

## Visuels (dossier `store/`)
| Fichier | Usage | Taille |
|---|---|---|
| `icone-512.png` | Icône haute résolution | 512 × 512 |
| `banniere-1024x500.png` | Image de présentation | 1024 × 500 |
| `capture-1.png` à `capture-6.png` | Captures téléphone | 1080 × 1920 |

Sources et rendu : `store/src/visuels.html` et `store/src/rendre.py` (Playwright local). La photo de terrasse est une image générée en local (FLUX), utilisée comme décor : elle ne représente aucun lieu réel.

## Formulaires de la console
- **Accès à l'appli** : tout est accessible sans connexion.
- **Annonces** : non, l'appli ne contient pas d'annonces.
- **Classification du contenu** : questionnaire IARC, catégorie « Utilitaire, productivité, communication ou autre ». Aucune violence, aucun jeu d'argent : aucune mise ni récompense réelle, le tirage désigne seulement qui paie l'addition.
- **Public cible** : 18 ans et plus.
- **Sécurité des données** : « L'appli ne collecte ni ne partage aucune donnée utilisateur ». Aucun chiffrement en transit nécessaire, car rien ne transite.
- **Appli d'actualités** : non. **Appli gouvernementale** : non. **Fonctions financières** : non (simple calculatrice, aucun paiement).
- **Santé** : non.

## Publication
1. Production → Créer une version → importer `app/build/outputs/bundle/release/app-release.aab`.
2. Signature d'application par Google Play : accepter (Google garde la clé principale ; notre clé d'importation est dans `%USERPROFILE%\.android-keys\chkoun\`, à sauvegarder sur une clé USB).
3. Pays : Maroc (ou tous les pays).
4. Notes de version : « Première version : roue, duel, secouer, partage de l'addition et bilan. »
