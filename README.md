# kosar
KOSAR is a cache manager for use with a data store (e.g., MySQL, Oracle) by an application.  A novel feature of KOSAR is its ability to author triggers once it encounters a unique query template.  It registers its triggers with the data store to receive notifications when DML commands change the data store.

Additional details can be found at:  
Shahram Ghandeharizadeh, Jason Yap.  SQL Query to Trigger Translation: A Novel Transparent Consistency Technique for Cache Augmented SQL Systems. DEXA Workshops 2017: 37-41
