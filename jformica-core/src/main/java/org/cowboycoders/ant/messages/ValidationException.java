/**
 * Copyright (c) 2013, Will Szumski
 * <p>
 * This file is part of formicidae.
 * <p>
 * formicidae is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * formicidae is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with formicidae.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 *
 */
package org.cowboycoders.ant.messages;

/**
 * Runtime exception, thrown if there was attempt to set a value
 * and the validation checks failed.
 * @author will
 *
 */
public class ValidationException extends FatalMessageException {

    /**
     *
     */
    private static final long serialVersionUID = -6928449140060446544L;

    /**
     *
     */
    public ValidationException() {
    }

    /**
     * @param detailMessage to document
     */
    public ValidationException(String detailMessage) {
        super(detailMessage);
    }

    /**
     * @param throwable to document
     */
    public ValidationException(Throwable throwable) {
        super(throwable);
    }

    /**
     * @param detailMessage to document
     * @param throwable to document
     */
    public ValidationException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

}
