UPDATE product
SET hero_image_url = replace(hero_image_url, 'your-bucket', 'plantahub-assets')
WHERE hero_image_url LIKE '%your-bucket%';

UPDATE product
SET hero_image_url = replace(hero_image_url, 's3.sa-east-1', 's3.us-east-2')
WHERE hero_image_url LIKE '%s3.sa-east-1%';