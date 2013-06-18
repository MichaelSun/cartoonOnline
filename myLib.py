import os
import shutil
import subprocess
import re

print 'Succes import myLib.py >>>>>>>>'

class FILE_NOT_EXISTS(Exception):
    """ file Not Exists
    """

class PROPERTY_NOT_EXISTS(Exception):
    """ property not find
    """

class CMD_EXEC_FAILURE(Exception):
    """ exec cmd failure
    """

class REPLACE_TEXT_FAILURE(Exception):
    """ replace text failure
    """

def replce_text_in_file(filename, oldText, newText):
    if os.path.exists(filename):
        #print '_replce_text_in_file : {0} oldText : {1} newText : {2}'.format(filename, oldText, newText)
        tmp = filename + '_tmp'
        file = open(filename)
        tmpFile = open(tmp, 'w+')
        for line in file:
            m = re.search(oldText, line)
            if m :
                rText = re.sub(oldText, newText, line)
                #print 'find match ', m.group(0), ' for oldText : ', oldText
                tmpFile.write(rText)
            else:
                tmpFile.write(line)

        tmpFile.flush()
        tmpFile.close()

        #print 'replace text success :  try to dump the {0}'.format(tmp)
        #_dump_file(tmp)

        shutil.move(tmp, filename)

    return REPLACE_TEXT_FAILURE()
