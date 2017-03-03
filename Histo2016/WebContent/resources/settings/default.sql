-- Stainings
INSERT INTO stainingprototype VALUES (1, false, '', 0, 'Trichrom-Masson', 0);
INSERT INTO stainingprototype VALUES (2, false, '', 1, 'Trichrom-Goldner', 0);
INSERT INTO stainingprototype VALUES (3, false, '', 2, 'AMP', 0);
INSERT INTO stainingprototype VALUES (4, false, '', 3, 'Kongorot', 0);
INSERT INTO stainingprototype VALUES (5, false, '', 4, 'Giemsa', 0);
INSERT INTO stainingprototype VALUES (6, false, '', 5, 'GMS', 0);
INSERT INTO stainingprototype VALUES (7, false, '', 6, 'Gram', 0);
INSERT INTO stainingprototype VALUES (8, false, '', 7, 'FE', 0);
INSERT INTO stainingprototype VALUES (9, false, '', 8, 'PAS', 0);
INSERT INTO stainingprototype VALUES (10, false, '', 9, 'van Gieson', 0);
INSERT INTO stainingprototype VALUES (11, false, '', 10, 'HE', 0);
INSERT INTO stainingprototype VALUES (12, false, '', 11, 'Melan A', 1);
INSERT INTO stainingprototype VALUES (13, false, '', 12, 'HMB 45', 1);
INSERT INTO stainingprototype VALUES (14, false, '', 13, 'Mib 1', 1);
INSERT INTO stainingprototype VALUES (15, false, '', 14, 'Vimentin', 1);
INSERT INTO stainingprototype VALUES (16, false, '', 15, 'Cytokeratin AE1-AE3', 1);
INSERT INTO stainingprototype VALUES (17, false, '', 16, 'GFAP', 1);
INSERT INTO stainingprototype VALUES (18, false, '', 17, 'S100', 1);
INSERT INTO stainingprototype VALUES (19, false, '', 18, 'NSE', 1);
INSERT INTO stainingprototype VALUES (20, false, '', 19, 'CD68', 1);
INSERT INTO stainingprototype VALUES (21, false, '', 20, 'CD34', 1);
INSERT INTO stainingprototype VALUES (22, false, '', 21, 'Faktor VIII', 1);

SELECT setval('stainingprototype_sequence', (SELECT MAX(id) FROM stainingprototype));

-- Material
INSERT INTO materialpreset VALUES (1, '', 0, 'Hautpr�parat');
INSERT INTO materialpreset VALUES (2, '', 1, 'Hornhaut');
INSERT INTO materialpreset VALUES (3, '', 2, 'Descemet');
INSERT INTO materialpreset VALUES (4, '', 3, 'Salzmannknoten');
INSERT INTO materialpreset VALUES (5, '', 4, 'Biopsie');
INSERT INTO materialpreset VALUES (6, '', 5, 'Bindehaut');
INSERT INTO materialpreset VALUES (7, '', 6, 'Lidkante');
INSERT INTO materialpreset VALUES (8, '', 7, 'Resektat');
INSERT INTO materialpreset VALUES (9, '', 8, 'Nachresektat');
INSERT INTO materialpreset VALUES (10, '', 9, 'Bulbus');
INSERT INTO materialpreset VALUES (11, '', 10, 'Eviszeratio');
INSERT INTO materialpreset VALUES (12, '', 11, 'Exenteration');
INSERT INTO materialpreset VALUES (13, '', 12, 'Zyste');
INSERT INTO materialpreset VALUES (14, '', 13, 'Arterie');
INSERT INTO materialpreset VALUES (15, '', 14, 'Bindehautabstrich');
INSERT INTO materialpreset VALUES (16, '', 15, 'Iris');
INSERT INTO materialpreset VALUES (17, '', 16, 'Linse');
INSERT INTO materialpreset VALUES (18, '', 17, 'Linsenkapsel');
INSERT INTO materialpreset VALUES (19, '', 18, 'Ziliark�rper');
INSERT INTO materialpreset VALUES (20, '', 19, 'Aderhaut');
INSERT INTO materialpreset VALUES (21, '', 20, 'Netzhaut');
INSERT INTO materialpreset VALUES (22, '', 21, 'Glask�rper');
INSERT INTO materialpreset VALUES (23, '', 22, 'Glask�rperaspirat');
INSERT INTO materialpreset VALUES (24, '', 23, 'Sklera');
INSERT INTO materialpreset VALUES (25, '', 24, 'Vorderkammer');
INSERT INTO materialpreset VALUES (26, '', 25, 'N. Optikus');
INSERT INTO materialpreset VALUES (27, '', 26, 'Keilexcisat');
INSERT INTO materialpreset VALUES (28, '', 27, 'Tr�nensack');
INSERT INTO materialpreset VALUES (29, '', 28, 'Augenmuskel');
INSERT INTO materialpreset VALUES (30, '', 29, 'Irisgewebe');
INSERT INTO materialpreset VALUES (31, '', 30, 'Orbitagewebe');
INSERT INTO materialpreset VALUES (32, '', 31, 'kein Material');
INSERT INTO materialpreset VALUES (33, '', 32, 'Paraffinblock');
INSERT INTO materialpreset VALUES (34, '', 33, 'Schnitte');

SELECT setval('materialPreset_sequence', (SELECT MAX(id) FROM materialpreset));


-- material -> stainings
INSERT INTO materialpreset_stainingprototype VALUES (1, 9);
INSERT INTO materialpreset_stainingprototype VALUES (1, 11);
INSERT INTO materialpreset_stainingprototype VALUES (2, 9);
INSERT INTO materialpreset_stainingprototype VALUES (2, 11);
INSERT INTO materialpreset_stainingprototype VALUES (3, 9);
INSERT INTO materialpreset_stainingprototype VALUES (3, 11);
INSERT INTO materialpreset_stainingprototype VALUES (4, 9);
INSERT INTO materialpreset_stainingprototype VALUES (4, 11);
INSERT INTO materialpreset_stainingprototype VALUES (5, 9);
INSERT INTO materialpreset_stainingprototype VALUES (5, 11);
INSERT INTO materialpreset_stainingprototype VALUES (6, 9);
INSERT INTO materialpreset_stainingprototype VALUES (6, 11);
INSERT INTO materialpreset_stainingprototype VALUES (7, 9);
INSERT INTO materialpreset_stainingprototype VALUES (7, 11);
INSERT INTO materialpreset_stainingprototype VALUES (8, 9);
INSERT INTO materialpreset_stainingprototype VALUES (8, 11);
INSERT INTO materialpreset_stainingprototype VALUES (9, 9);
INSERT INTO materialpreset_stainingprototype VALUES (9, 11);
INSERT INTO materialpreset_stainingprototype VALUES (10, 9);
INSERT INTO materialpreset_stainingprototype VALUES (10, 11);
INSERT INTO materialpreset_stainingprototype VALUES (11, 9);
INSERT INTO materialpreset_stainingprototype VALUES (11, 11);
INSERT INTO materialpreset_stainingprototype VALUES (12, 9);
INSERT INTO materialpreset_stainingprototype VALUES (12, 11);
INSERT INTO materialpreset_stainingprototype VALUES (13, 9);
INSERT INTO materialpreset_stainingprototype VALUES (13, 11);
INSERT INTO materialpreset_stainingprototype VALUES (14, 9);
INSERT INTO materialpreset_stainingprototype VALUES (14, 11);
INSERT INTO materialpreset_stainingprototype VALUES (15, 9);
INSERT INTO materialpreset_stainingprototype VALUES (15, 11);
INSERT INTO materialpreset_stainingprototype VALUES (16, 9);
INSERT INTO materialpreset_stainingprototype VALUES (16, 11);
INSERT INTO materialpreset_stainingprototype VALUES (17, 9);
INSERT INTO materialpreset_stainingprototype VALUES (17, 11);
INSERT INTO materialpreset_stainingprototype VALUES (18, 9);
INSERT INTO materialpreset_stainingprototype VALUES (18, 11);
INSERT INTO materialpreset_stainingprototype VALUES (19, 9);
INSERT INTO materialpreset_stainingprototype VALUES (19, 11);
INSERT INTO materialpreset_stainingprototype VALUES (20, 9);
INSERT INTO materialpreset_stainingprototype VALUES (20, 11);
INSERT INTO materialpreset_stainingprototype VALUES (21, 9);
INSERT INTO materialpreset_stainingprototype VALUES (21, 11);
INSERT INTO materialpreset_stainingprototype VALUES (22, 9);
INSERT INTO materialpreset_stainingprototype VALUES (22, 11);
INSERT INTO materialpreset_stainingprototype VALUES (23, 9);
INSERT INTO materialpreset_stainingprototype VALUES (23, 11);
INSERT INTO materialpreset_stainingprototype VALUES (24, 9);
INSERT INTO materialpreset_stainingprototype VALUES (24, 11);
INSERT INTO materialpreset_stainingprototype VALUES (25, 9);
INSERT INTO materialpreset_stainingprototype VALUES (25, 11);
INSERT INTO materialpreset_stainingprototype VALUES (26, 9);
INSERT INTO materialpreset_stainingprototype VALUES (26, 11);
INSERT INTO materialpreset_stainingprototype VALUES (27, 9);
INSERT INTO materialpreset_stainingprototype VALUES (27, 11);
INSERT INTO materialpreset_stainingprototype VALUES (28, 9);
INSERT INTO materialpreset_stainingprototype VALUES (28, 11);
INSERT INTO materialpreset_stainingprototype VALUES (29, 9);
INSERT INTO materialpreset_stainingprototype VALUES (29, 11);
INSERT INTO materialpreset_stainingprototype VALUES (30, 9);
INSERT INTO materialpreset_stainingprototype VALUES (30, 11);
INSERT INTO materialpreset_stainingprototype VALUES (31, 9);
INSERT INTO materialpreset_stainingprototype VALUES (31, 11);
INSERT INTO materialpreset_stainingprototype VALUES (33, 9);
INSERT INTO materialpreset_stainingprototype VALUES (33, 11);
INSERT INTO materialpreset_stainingprototype VALUES (34, 9);
INSERT INTO materialpreset_stainingprototype VALUES (34, 11);

-- history
INSERT INTO listitem VALUES (1, false, 0, 1, 'V. a. Chalazion');
INSERT INTO listitem VALUES (2, false, 1, 1, 'V.a. Chalazion/pyogenes Granulom');
INSERT INTO listitem VALUES (3, false, 2, 1, 'V.a. pyogenes Granulom');
INSERT INTO listitem VALUES (4, false, 3, 1, 'V. a. Basaliom');
INSERT INTO listitem VALUES (5, false, 4, 1, 'Nachresektion bei Basaliom');
INSERT INTO listitem VALUES (6, false, 5, 1, 'Nachresektion z. Vergr��erung Sicherheitsabstand');
INSERT INTO listitem VALUES (7, false, 6, 1, 'V. a. Papillom');
INSERT INTO listitem VALUES (8, false, 7, 1, 'V.a. Pterygium');
INSERT INTO listitem VALUES (9, false, 8, 1, 'V.a. Verruca vulg.');
INSERT INTO listitem VALUES (10, false, 9, 1, 'Lidrandzyste');
INSERT INTO listitem VALUES (11, false, 10, 1, 'V. a. CIN');
INSERT INTO listitem VALUES (12, false, 11, 1, 'Xanthelasma');
INSERT INTO listitem VALUES (13, false, 12, 1, 'V. a. aktinische Keratose');
INSERT INTO listitem VALUES (14, false, 13, 1, 'V. a. Epidermiszyste');
INSERT INTO listitem VALUES (15, false, 14, 1, 'V. a. Dermoidzyste');
INSERT INTO listitem VALUES (16, false, 15, 1, 'V.a. N�vus');
INSERT INTO listitem VALUES (17, false, 16, 1, 'V. a. Bindehautn�vus');
INSERT INTO listitem VALUES (18, false, 17, 1, 'V. a. Lymphom');
INSERT INTO listitem VALUES (19, false, 18, 1, 'Keil bei Blaskovic');
INSERT INTO listitem VALUES (20, false, 19, 1, 'V. a. Mollzyste');
INSERT INTO listitem VALUES (21, false, 20, 1, 'V. a. Arteriitis temporalis');
INSERT INTO listitem VALUES (22, false, 21, 1, 'Keratokonus');
INSERT INTO listitem VALUES (23, false, 22, 1, 'V. a. Salzmannknoten');
INSERT INTO listitem VALUES (24, false, 23, 1, 'DMEK');
INSERT INTO listitem VALUES (25, false, 24, 1, 'Descemet bei Fuchs');
INSERT INTO listitem VALUES (26, false, 25, 1, 'Bullosa');
INSERT INTO listitem VALUES (27, false, 26, 1, 'Endothelversagen');
INSERT INTO listitem VALUES (28, false, 27, 1, 'Fuchs''sche Endotheldystrophie');
INSERT INTO listitem VALUES (29, false, 28, 1, 'Limbusinsuffizienz');
INSERT INTO listitem VALUES (30, false, 29, 1, 'Z. n. Herpes');
INSERT INTO listitem VALUES (31, false, 30, 1, 'Herpesnarbe');
INSERT INTO listitem VALUES (32, false, 31, 1, 'Transplantatversagen');
INSERT INTO listitem VALUES (33, false, 32, 1, 'Re-Keratokonus');
INSERT INTO listitem VALUES (34, false, 33, 1, 'bull�se Keratopathie');
INSERT INTO listitem VALUES (35, false, 34, 1, 'Pinguecula');

SELECT setval('listItem_sequence', (SELECT MAX(id) FROM listitem));