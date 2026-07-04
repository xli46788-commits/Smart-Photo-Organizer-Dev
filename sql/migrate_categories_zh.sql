-- 将历史英文分类迁移为中文
USE imprint;

UPDATE photo SET ai_category = '节日' WHERE ai_category = 'FESTIVAL';
UPDATE photo SET ai_category = '风景' WHERE ai_category = 'SCENE';
UPDATE photo SET ai_category = '设计' WHERE ai_category = 'DESIGN';
UPDATE photo SET ai_category = '人像' WHERE ai_category = 'BEAUTY';
UPDATE photo SET ai_category = '动漫' WHERE ai_category = 'ANIME';
UPDATE photo SET ai_category = '食物' WHERE ai_category = 'FOOD';
UPDATE photo SET ai_category = '动物' WHERE ai_category = 'ANIMAL';
UPDATE photo SET ai_category = '其他' WHERE ai_category = 'OTHER';
