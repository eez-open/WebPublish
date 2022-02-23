/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.DOM;

import java.util.List;

/**
 *
 * @author martin
 */
abstract public class Filter extends Node {
    public Filter(Document doc) {
        super(doc);
    }

    abstract public List<Block> process(BlockContainer parent, List<Block> blocks);
}
